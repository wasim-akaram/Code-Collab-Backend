package com.codesync.payment.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.codesync.payment.dto.*;
import com.codesync.payment.service.PaymentService;
import com.razorpay.RazorpayException;

/**
 * Unit tests for {@link PaymentController}.
 * Uses pure Mockito (no Spring context) for fast, isolated controller testing.
 * Each test verifies the controller's HTTP response logic directly.
 */
@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock private PaymentService paymentService;
    @InjectMocks private PaymentController controller;

    // ─── GET /payments/plans ───────────────────────────────────────────

    @Test
    @DisplayName("getPlans should return 200 with plan list")
    void getPlans_shouldReturnOk() {
        List<PlanDto> plans = List.of(
            PlanDto.builder().planId("FREE").name("Free").amountPaise(0).build(),
            PlanDto.builder().planId("PRO_MONTHLY").name("Pro").amountPaise(49900).build()
        );
        when(paymentService.getAvailablePlans()).thenReturn(plans);

        ResponseEntity<List<PlanDto>> response = controller.getPlans();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        assertEquals("FREE", response.getBody().get(0).getPlanId());
    }

    // ─── POST /payments/create-order ──────────────────────────────────

    @Test
    @DisplayName("createOrder without email should return 401")
    void createOrder_nullEmail_shouldReturn401() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setPlanId("PRO_MONTHLY");

        ResponseEntity<?> response = controller.createOrder(null, req);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("createOrder with blank email should return 401")
    void createOrder_blankEmail_shouldReturn401() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setPlanId("PRO_MONTHLY");

        ResponseEntity<?> response = controller.createOrder("  ", req);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("createOrder with valid email should return 200 with order details")
    void createOrder_validEmail_shouldReturnOrder() throws Exception {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setPlanId("PRO_MONTHLY");
        CreateOrderResponse expected = CreateOrderResponse.builder()
                .orderId("order_123").amountPaise(49900).currency("INR").build();

        when(paymentService.createOrder(eq("user@test.com"), any())).thenReturn(expected);

        ResponseEntity<?> response = controller.createOrder("user@test.com", req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        CreateOrderResponse body = (CreateOrderResponse) response.getBody();
        assertEquals("order_123", body.getOrderId());
    }

    @Test
    @DisplayName("createOrder exception should return 500")
    void createOrder_exception_shouldReturn500() throws Exception {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setPlanId("PRO_MONTHLY");
        when(paymentService.createOrder(any(), any())).thenThrow(new RazorpayException("API fail"));

        ResponseEntity<?> response = controller.createOrder("user@test.com", req);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ─── POST /payments/verify ────────────────────────────────────────

    @Test
    @DisplayName("verifyPayment without email should return 401")
    void verifyPayment_nullEmail_shouldReturn401() {
        VerifyPaymentRequest req = createVerifyRequest();

        ResponseEntity<?> response = controller.verifyPayment(null, req);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("verifyPayment with blank email should return 401")
    void verifyPayment_blankEmail_shouldReturn401() {
        VerifyPaymentRequest req = createVerifyRequest();

        ResponseEntity<?> response = controller.verifyPayment("  ", req);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("verifyPayment with valid data should return subscription status")
    void verifyPayment_valid_shouldReturnStatus() {
        VerifyPaymentRequest req = createVerifyRequest();
        SubscriptionStatusDto expected = SubscriptionStatusDto.builder()
                .plan("PRO").status("ACTIVE").active(true)
                .startDate(Instant.now()).endDate(Instant.now().plusSeconds(86400 * 30)).build();

        when(paymentService.verifyPayment(eq("user@test.com"), any())).thenReturn(expected);

        ResponseEntity<?> response = controller.verifyPayment("user@test.com", req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("verifyPayment exception should return 500")
    void verifyPayment_exception_shouldReturn500() {
        VerifyPaymentRequest req = createVerifyRequest();
        when(paymentService.verifyPayment(any(), any())).thenThrow(new RuntimeException("Bad sig"));

        ResponseEntity<?> response = controller.verifyPayment("user@test.com", req);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ─── GET /payments/subscription ───────────────────────────────────

    @Test
    @DisplayName("getSubscription without email should return 401")
    void getSubscription_nullEmail_shouldReturn401() {
        ResponseEntity<?> response = controller.getSubscription(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("getSubscription with blank email should return 401")
    void getSubscription_blankEmail_shouldReturn401() {
        ResponseEntity<?> response = controller.getSubscription("  ");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("getSubscription for free user should return FREE plan")
    void getSubscription_freeUser_shouldReturnFree() {
        SubscriptionStatusDto free = SubscriptionStatusDto.builder()
                .plan("FREE").status("NONE").active(false).build();
        when(paymentService.getSubscriptionStatus("free@test.com")).thenReturn(free);

        ResponseEntity<?> response = controller.getSubscription("free@test.com");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("getSubscription exception should return 500")
    void getSubscription_exception_shouldReturn500() {
        when(paymentService.getSubscriptionStatus(any())).thenThrow(new RuntimeException("DB error"));

        ResponseEntity<?> response = controller.getSubscription("user@test.com");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    /** Creates a VerifyPaymentRequest with test data. */
    private VerifyPaymentRequest createVerifyRequest() {
        VerifyPaymentRequest r = new VerifyPaymentRequest();
        r.setRazorpayOrderId("order_test");
        r.setRazorpayPaymentId("pay_test");
        r.setRazorpaySignature("sig_test");
        return r;
    }
}
