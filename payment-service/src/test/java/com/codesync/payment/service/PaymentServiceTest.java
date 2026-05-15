package com.codesync.payment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codesync.payment.client.NotificationClient;
import com.codesync.payment.dto.*;
import com.codesync.payment.entity.PaymentTransaction;
import com.codesync.payment.entity.Subscription;
import com.codesync.payment.repository.PaymentTransactionRepository;
import com.codesync.payment.repository.SubscriptionRepository;

/**
 * Unit tests for {@link PaymentService}.
 * Tests business logic for plan retrieval, subscription status,
 * payment verification, and internal helper methods.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final String TEST_SECRET = "test_secret_key_for_unit_tests_only";

    @Mock private SubscriptionRepository subscriptionRepo;
    @Mock private PaymentTransactionRepository transactionRepo;
    @Mock private NotificationClient notificationClient;

    private PaymentService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new PaymentService(
                "rzp_test_000000000000000",
                TEST_SECRET,
                "http://localhost:9090",
                subscriptionRepo,
                transactionRepo,
                notificationClient
        );
    }

    // ─── getAvailablePlans() ──────────────────────────────────────────

    @Test
    @DisplayName("getAvailablePlans should return FREE and PRO plans")
    void getAvailablePlans_returnsTwoPlans() {
        var plans = service.getAvailablePlans();
        assertEquals(2, plans.size());
        assertEquals("FREE", plans.get(0).getPlanId());
        assertEquals("PRO_MONTHLY", plans.get(1).getPlanId());
        assertEquals(0, plans.get(0).getAmountPaise());
        assertEquals(49900, plans.get(1).getAmountPaise());
        assertTrue(plans.get(1).getFeatures().contains("Unlimited projects"));
        assertEquals("INR", plans.get(1).getCurrency());
        assertEquals(30, plans.get(1).getDurationDays());
    }

    @Test
    @DisplayName("FREE plan should have expected features list")
    void getAvailablePlans_freePlanFeatures() {
        var features = service.getAvailablePlans().get(0).getFeatures();
        assertTrue(features.contains("Up to 5 projects"));
        assertTrue(features.contains("In-app notifications"));
    }

    // ─── getSubscriptionStatus() ──────────────────────────────────────

    @Test
    @DisplayName("No subscription should return FREE/NONE")
    void getSubscriptionStatus_noSub_returnsFree() {
        when(subscriptionRepo.findTopByUserEmailAndStatusOrderByEndDateDesc("u@t.com", "ACTIVE"))
                .thenReturn(Optional.empty());

        SubscriptionStatusDto s = service.getSubscriptionStatus("u@t.com");
        assertEquals("FREE", s.getPlan());
        assertEquals("NONE", s.getStatus());
        assertFalse(s.isActive());
    }

    @Test
    @DisplayName("Active PRO subscription should return ACTIVE")
    void getSubscriptionStatus_activePro() {
        Subscription sub = Subscription.builder()
                .plan("PRO").status("ACTIVE")
                .startDate(Instant.now().minus(5, ChronoUnit.DAYS))
                .endDate(Instant.now().plus(25, ChronoUnit.DAYS)).build();
        when(subscriptionRepo.findTopByUserEmailAndStatusOrderByEndDateDesc("p@t.com", "ACTIVE"))
                .thenReturn(Optional.of(sub));

        SubscriptionStatusDto s = service.getSubscriptionStatus("p@t.com");
        assertEquals("PRO", s.getPlan());
        assertEquals("ACTIVE", s.getStatus());
        assertTrue(s.isActive());
    }

    @Test
    @DisplayName("Expired subscription should mark EXPIRED and downgrade to FREE")
    void getSubscriptionStatus_expired() {
        Subscription sub = Subscription.builder()
                .plan("PRO").status("ACTIVE")
                .startDate(Instant.now().minus(35, ChronoUnit.DAYS))
                .endDate(Instant.now().minus(5, ChronoUnit.DAYS)).build();
        when(subscriptionRepo.findTopByUserEmailAndStatusOrderByEndDateDesc("e@t.com", "ACTIVE"))
                .thenReturn(Optional.of(sub));
        when(subscriptionRepo.save(any())).thenReturn(sub);

        SubscriptionStatusDto s = service.getSubscriptionStatus("e@t.com");
        assertEquals("EXPIRED", s.getStatus());
        assertFalse(s.isActive());
        verify(subscriptionRepo).save(any(Subscription.class));
    }

    @Test
    @DisplayName("Subscription with null endDate should be treated as expired")
    void getSubscriptionStatus_nullEndDate() {
        Subscription sub = Subscription.builder()
                .plan("PRO").status("ACTIVE")
                .startDate(Instant.now()).endDate(null).build();
        when(subscriptionRepo.findTopByUserEmailAndStatusOrderByEndDateDesc("n@t.com", "ACTIVE"))
                .thenReturn(Optional.of(sub));
        when(subscriptionRepo.save(any())).thenReturn(sub);

        SubscriptionStatusDto s = service.getSubscriptionStatus("n@t.com");
        assertFalse(s.isActive());
    }

    // ─── verifyPayment() ──────────────────────────────────────────────

    @Test
    @DisplayName("verifyPayment with valid HMAC signature should activate PRO")
    void verifyPayment_validSignature_activatesPro() {
        // Generate a real HMAC signature using the test secret
        String orderId = "order_test123";
        String paymentId = "pay_test456";
        String signature = generateHmac(orderId + "|" + paymentId, TEST_SECRET);

        VerifyPaymentRequest req = new VerifyPaymentRequest();
        req.setRazorpayOrderId(orderId);
        req.setRazorpayPaymentId(paymentId);
        req.setRazorpaySignature(signature);

        // Mock the transaction lookup
        PaymentTransaction txn = PaymentTransaction.builder()
                .userEmail("user@test.com").razorpayOrderId(orderId).status("CREATED").build();
        when(transactionRepo.findByRazorpayOrderId(orderId)).thenReturn(Optional.of(txn));
        when(transactionRepo.save(any())).thenReturn(txn);
        when(subscriptionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionStatusDto result = service.verifyPayment("user@test.com", req);

        assertEquals("PRO", result.getPlan());
        assertEquals("ACTIVE", result.getStatus());
        assertTrue(result.isActive());
        assertNotNull(result.getStartDate());
        assertNotNull(result.getEndDate());
        verify(transactionRepo).save(any());
        verify(subscriptionRepo).save(any());
        verify(notificationClient).sendProActivatedNotification("user@test.com");
        verify(notificationClient).sendPaymentReceivedNotification("user@test.com", 49900, orderId);
    }

    @Test
    @DisplayName("verifyPayment with invalid signature should throw")
    void verifyPayment_invalidSignature_throws() {
        VerifyPaymentRequest req = new VerifyPaymentRequest();
        req.setRazorpayOrderId("order_x");
        req.setRazorpayPaymentId("pay_x");
        req.setRazorpaySignature("invalid_signature");

        assertThrows(RuntimeException.class,
                () -> service.verifyPayment("user@test.com", req));
    }

    @Test
    @DisplayName("verifyPayment with no existing transaction should still create subscription")
    void verifyPayment_noTransaction_stillCreates() {
        String orderId = "order_new";
        String paymentId = "pay_new";
        String signature = generateHmac(orderId + "|" + paymentId, TEST_SECRET);

        VerifyPaymentRequest req = new VerifyPaymentRequest();
        req.setRazorpayOrderId(orderId);
        req.setRazorpayPaymentId(paymentId);
        req.setRazorpaySignature(signature);

        when(transactionRepo.findByRazorpayOrderId(orderId)).thenReturn(Optional.empty());
        when(subscriptionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionStatusDto result = service.verifyPayment("u@t.com", req);
        assertEquals("PRO", result.getPlan());
        assertTrue(result.isActive());
    }

    // ─── verifyRazorpaySignature (via reflection) ─────────────────────

    @Test
    @DisplayName("verifyRazorpaySignature should return true for correct HMAC")
    void verifySignature_correctHmac() throws Exception {
        Method m = PaymentService.class.getDeclaredMethod(
                "verifyRazorpaySignature", String.class, String.class, String.class);
        m.setAccessible(true);

        String sig = generateHmac("ord|pay", TEST_SECRET);
        assertTrue((Boolean) m.invoke(service, "ord", "pay", sig));
    }

    @Test
    @DisplayName("verifyRazorpaySignature should return false for wrong signature")
    void verifySignature_wrongHmac() throws Exception {
        Method m = PaymentService.class.getDeclaredMethod(
                "verifyRazorpaySignature", String.class, String.class, String.class);
        m.setAccessible(true);
        assertFalse((Boolean) m.invoke(service, "ord", "pay", "wrong"));
    }

    // ─── bytesToHex (via reflection) ──────────────────────────────────

    @Test
    @DisplayName("bytesToHex should convert bytes to hex string")
    void bytesToHex_converts() throws Exception {
        Method m = PaymentService.class.getDeclaredMethod("bytesToHex", byte[].class);
        m.setAccessible(true);
        assertEquals("00ff7f", m.invoke(null, new byte[]{0, (byte) 0xff, 127}));
    }

    // ─── updateAuthServicePlan (via reflection) ───────────────────────

    @Test
    @DisplayName("updateAuthServicePlan should not throw when auth-service is down")
    void updateAuthServicePlan_serviceDown_doesNotThrow() throws Exception {
        Method m = PaymentService.class.getDeclaredMethod(
                "updateAuthServicePlan", String.class, String.class, Instant.class);
        m.setAccessible(true);
        // auth-service is not running — should log error but not throw
        assertDoesNotThrow(() -> m.invoke(service, "u@t.com", "PRO", Instant.now()));
    }

    @Test
    @DisplayName("updateAuthServicePlan with null expiresAt should work")
    void updateAuthServicePlan_nullExpires() throws Exception {
        Method m = PaymentService.class.getDeclaredMethod(
                "updateAuthServicePlan", String.class, String.class, Instant.class);
        m.setAccessible(true);
        assertDoesNotThrow(() -> m.invoke(service, "u@t.com", "FREE", null));
    }

    // ─── Helper ───────────────────────────────────────────────────────

    /** Generates a real HMAC-SHA256 hex string, mimicking Razorpay's signature. */
    private String generateHmac(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
