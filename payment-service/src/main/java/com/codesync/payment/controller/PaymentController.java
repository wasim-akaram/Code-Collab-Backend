package com.codesync.payment.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.codesync.payment.dto.*;
import com.codesync.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** Public — returns available plans and pricing. */
    @GetMapping("/plans")
    public ResponseEntity<List<PlanDto>> getPlans() {
        return ResponseEntity.ok(paymentService.getAvailablePlans());
    }

    /** Auth required — creates a Razorpay order for checkout. */
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
            @RequestHeader(value = "X-User", required = false) String email,
            @RequestBody CreateOrderRequest request) {

        if (email == null || email.isBlank()) {
            log.warn("POST /create-order called without X-User header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required"));
        }

        try {
            log.info("Creating order for user={} plan={}", email, request.getPlanId());
            CreateOrderResponse response = paymentService.createOrder(email, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[PAYMENT] Failed to create order for user={}: {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to create order: " + e.getMessage()));
        }
    }

    /** Auth required — verifies payment and activates subscription. */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @RequestHeader(value = "X-User", required = false) String email,
            @RequestBody VerifyPaymentRequest request) {

        if (email == null || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required"));
        }

        try {
            log.info("Verifying payment for user={}", email);
            return ResponseEntity.ok(paymentService.verifyPayment(email, request));
        } catch (Exception e) {
            log.error("[PAYMENT] Failed to verify payment for user={}: {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Payment verification failed: " + e.getMessage()));
        }
    }

    /** Auth required — returns current subscription status. */
    @GetMapping("/subscription")
    public ResponseEntity<?> getSubscription(
            @RequestHeader(value = "X-User", required = false) String email) {

        if (email == null || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required"));
        }

        try {
            return ResponseEntity.ok(paymentService.getSubscriptionStatus(email));
        } catch (Exception e) {
            log.error("[PAYMENT] Failed to get subscription for user={}: {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to get subscription: " + e.getMessage()));
        }
    }
}
