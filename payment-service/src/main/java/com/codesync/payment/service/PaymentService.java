/*
 * Code reader note: Handles plan discovery, payment order creation, signature
 * verification, subscription activation, and auth-service plan updates.
 * Annotations used: @Service registers the service bean, @Slf4j provides logging,
 * and @Value injects configuration values into the constructor.
 */
package com.codesync.payment.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.codesync.payment.client.NotificationClient;
import com.codesync.payment.dto.*;
import com.codesync.payment.entity.PaymentTransaction;
import com.codesync.payment.entity.Subscription;
import com.codesync.payment.repository.PaymentTransactionRepository;
import com.codesync.payment.repository.SubscriptionRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final String razorpayKeyId;
    private final String razorpayKeySecret;
    private final SubscriptionRepository subscriptionRepo;
    private final PaymentTransactionRepository transactionRepo;
    private final RestTemplate restTemplate;
    private final String authServiceUrl;
    private final NotificationClient notificationClient;

    // Pro plan: ₹499/month = 49900 paise
    private static final int PRO_MONTHLY_AMOUNT = 49900;
    private static final String CURRENCY = "INR";
    private static final int PRO_DURATION_DAYS = 30;

    public PaymentService(
            @Value("${razorpay.key-id}") String keyId,
            @Value("${razorpay.key-secret}") String keySecret,
            @Value("${auth-service.url}") String authServiceUrl,
            SubscriptionRepository subscriptionRepo,
            PaymentTransactionRepository transactionRepo,
            NotificationClient notificationClient
    ) throws RazorpayException {
        this.notificationClient = notificationClient;
        this.razorpayKeyId = keyId;
        this.razorpayKeySecret = keySecret;
        this.authServiceUrl = authServiceUrl;
        this.subscriptionRepo = subscriptionRepo;
        this.transactionRepo = transactionRepo;

        // Initialize Razorpay client
        this.razorpayClient = new RazorpayClient(keyId, keySecret);

        // RestTemplate with timeouts for auth-service calls
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Returns available plans for the pricing page.
     */
    public List<PlanDto> getAvailablePlans() {
        return List.of(
            PlanDto.builder()
                .planId("FREE")
                .name("Free")
                .description("Get started with CodeSync")
                .amountPaise(0)
                .currency(CURRENCY)
                .durationDays(0)
                .features(List.of(
                    "Up to 5 projects",
                    "1 private project",
                    "Code execution (10s timeout)",
                    "3 collaborators per session",
                    "20 snapshots per file",
                    "1 MB file upload limit",
                    "In-app notifications"
                ))
                .build(),
            PlanDto.builder()
                .planId("PRO_MONTHLY")
                .name("Pro")
                .description("Unlock the full power of CodeSync")
                .amountPaise(PRO_MONTHLY_AMOUNT)
                .currency(CURRENCY)
                .durationDays(PRO_DURATION_DAYS)
                .features(List.of(
                    "Unlimited projects",
                    "Unlimited private projects",
                    "Code execution (60s timeout)",
                    "20 collaborators per session",
                    "Unlimited snapshots",
                    "10 MB file upload limit",
                    "Priority email notifications",
                    "Pro badge on profile"
                ))
                .build()
        );
    }

    /**
     * Creates a Razorpay order for Pro subscription.
     */
    public CreateOrderResponse createOrder(String userEmail, CreateOrderRequest request) throws RazorpayException {
        log.info("[PAYMENT] Creating order for user={}, plan={}", userEmail, request.getPlanId());

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", PRO_MONTHLY_AMOUNT);
        orderRequest.put("currency", CURRENCY);
        // Razorpay receipt max length = 40 chars. Use short hash of email + timestamp suffix.
        String shortEmail = userEmail.length() > 15 ? userEmail.substring(0, 15) : userEmail;
        String receipt = "pro_" + shortEmail + "_" + (System.currentTimeMillis() % 1_000_000_000);
        orderRequest.put("receipt", receipt.length() > 40 ? receipt.substring(0, 40) : receipt);
        orderRequest.put("notes", new JSONObject(Map.of("email", userEmail, "plan", "PRO_MONTHLY")));

        Order order = razorpayClient.orders.create(orderRequest);
        String orderId = order.get("id");

        // Save transaction record
        transactionRepo.save(PaymentTransaction.builder()
                .userEmail(userEmail)
                .razorpayOrderId(orderId)
                .amountPaise(PRO_MONTHLY_AMOUNT)
                .currency(CURRENCY)
                .status("CREATED")
                .build());

        log.info("[PAYMENT] Order created: orderId={}", orderId);

        return CreateOrderResponse.builder()
                .orderId(orderId)
                .amountPaise(PRO_MONTHLY_AMOUNT)
                .currency(CURRENCY)
                .razorpayKeyId(razorpayKeyId)
                .planName("CodeSync Pro — Monthly")
                .description("Unlock unlimited projects, extended execution, and more")
                .build();
    }

    /**
     * Verifies payment signature and activates Pro subscription.
     */
    public SubscriptionStatusDto verifyPayment(String userEmail, VerifyPaymentRequest request) {
        log.info("[PAYMENT] Verifying payment for user={}, orderId={}", userEmail, request.getRazorpayOrderId());

        // 1. Verify Razorpay signature
        boolean valid = verifyRazorpaySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (!valid) {
            log.error("[PAYMENT] Invalid signature for orderId={}", request.getRazorpayOrderId());
            throw new RuntimeException("Payment verification failed — invalid signature");
        }

        // 2. Update transaction record
        transactionRepo.findByRazorpayOrderId(request.getRazorpayOrderId()).ifPresent(txn -> {
            txn.setRazorpayPaymentId(request.getRazorpayPaymentId());
            txn.setRazorpaySignature(request.getRazorpaySignature());
            txn.setStatus("SUCCESS");
            transactionRepo.save(txn);
        });

        // 3. Create subscription
        Instant now = Instant.now();
        Instant endDate = now.plus(PRO_DURATION_DAYS, ChronoUnit.DAYS);

        Subscription sub = subscriptionRepo.save(Subscription.builder()
                .userEmail(userEmail)
                .plan("PRO")
                .status("ACTIVE")
                .amountPaise(PRO_MONTHLY_AMOUNT)
                .startDate(now)
                .endDate(endDate)
                .razorpayOrderId(request.getRazorpayOrderId())
                .razorpayPaymentId(request.getRazorpayPaymentId())
                .build());

        // 4. Update user plan in auth-service
        updateAuthServicePlan(userEmail, "PRO", endDate);

        // 5. Send notifications
        notificationClient.sendProActivatedNotification(userEmail);
        notificationClient.sendPaymentReceivedNotification(userEmail, PRO_MONTHLY_AMOUNT, request.getRazorpayOrderId());

        log.info("[PAYMENT] Pro activated for user={}, expires={}", userEmail, endDate);

        return SubscriptionStatusDto.builder()
                .plan("PRO")
                .status("ACTIVE")
                .startDate(now)
                .endDate(endDate)
                .active(true)
                .build();
    }

    /**
     * Returns the current subscription status for a user.
     */
    public SubscriptionStatusDto getSubscriptionStatus(String userEmail) {
        return subscriptionRepo.findTopByUserEmailAndStatusOrderByEndDateDesc(userEmail, "ACTIVE")
                .map(sub -> {
                    boolean isActive = sub.getEndDate() != null && sub.getEndDate().isAfter(Instant.now());
                    if (!isActive && "ACTIVE".equals(sub.getStatus())) {
                        sub.setStatus("EXPIRED");
                        subscriptionRepo.save(sub);
                        updateAuthServicePlan(userEmail, "FREE", null);
                    }
                    return SubscriptionStatusDto.builder()
                            .plan(sub.getPlan())
                            .status(isActive ? "ACTIVE" : "EXPIRED")
                            .startDate(sub.getStartDate())
                            .endDate(sub.getEndDate())
                            .active(isActive)
                            .build();
                })
                .orElse(SubscriptionStatusDto.builder()
                        .plan("FREE")
                        .status("NONE")
                        .active(false)
                        .build());
    }

    // ─── Internal helpers ───────────────────────────────────────────────────────

    private boolean verifyRazorpaySignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes());
            String generatedSignature = bytesToHex(hash);
            return generatedSignature.equals(signature);
        } catch (Exception e) {
            log.error("[PAYMENT] Signature verification error", e);
            return false;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void updateAuthServicePlan(String userEmail, String plan, Instant expiresAt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new java.util.HashMap<>();
            body.put("email", userEmail);
            body.put("plan", plan);
            if (expiresAt != null) {
                body.put("expiresAt", expiresAt.toString());
            }

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
            restTemplate.exchange(
                    authServiceUrl + "/auth/plan",
                    HttpMethod.PUT,
                    entity,
                    Object.class
            );
            log.info("[PAYMENT] Updated auth-service plan for user={} to {}", userEmail, plan);
        } catch (Exception e) {
            log.error("[PAYMENT] Failed to update auth-service plan for {}: {}", userEmail, e.getMessage(), e);
        }
    }
}
