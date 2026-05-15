package com.codesync.payment.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for all payment DTOs.
 * Exercises Lombok-generated methods to ensure coverage.
 */
class DtoTest {

    @Test
    @DisplayName("PlanDto builder and getters")
    void planDto() {
        PlanDto dto = PlanDto.builder()
                .planId("PRO").name("Pro").description("Desc")
                .amountPaise(49900).currency("INR").durationDays(30)
                .features(List.of("f1", "f2")).build();
        assertEquals("PRO", dto.getPlanId());
        assertEquals("Pro", dto.getName());
        assertEquals("Desc", dto.getDescription());
        assertEquals(49900, dto.getAmountPaise());
        assertEquals("INR", dto.getCurrency());
        assertEquals(30, dto.getDurationDays());
        assertEquals(2, dto.getFeatures().size());
        assertNotNull(dto.toString());
        assertEquals(dto, PlanDto.builder().planId("PRO").name("Pro")
                .description("Desc").amountPaise(49900).currency("INR")
                .durationDays(30).features(List.of("f1", "f2")).build());
    }

    @Test
    @DisplayName("CreateOrderRequest getters/setters")
    void createOrderRequest() {
        CreateOrderRequest r = new CreateOrderRequest();
        r.setPlanId("PRO_MONTHLY");
        assertEquals("PRO_MONTHLY", r.getPlanId());
        assertNotNull(r.toString());
        CreateOrderRequest r2 = new CreateOrderRequest();
        r2.setPlanId("PRO_MONTHLY");
        assertEquals(r, r2);
        r.hashCode(); // exercise hashCode
    }

    @Test
    @DisplayName("CreateOrderResponse builder and getters")
    void createOrderResponse() {
        CreateOrderResponse r = CreateOrderResponse.builder()
                .orderId("o1").amountPaise(49900).currency("INR")
                .razorpayKeyId("key").planName("Pro").description("d").build();
        assertEquals("o1", r.getOrderId());
        assertEquals(49900, r.getAmountPaise());
        assertEquals("INR", r.getCurrency());
        assertEquals("key", r.getRazorpayKeyId());
        assertEquals("Pro", r.getPlanName());
        assertEquals("d", r.getDescription());
        assertNotNull(r.toString());
    }

    @Test
    @DisplayName("VerifyPaymentRequest getters/setters")
    void verifyPaymentRequest() {
        VerifyPaymentRequest r = new VerifyPaymentRequest();
        r.setRazorpayOrderId("o1");
        r.setRazorpayPaymentId("p1");
        r.setRazorpaySignature("s1");
        assertEquals("o1", r.getRazorpayOrderId());
        assertEquals("p1", r.getRazorpayPaymentId());
        assertEquals("s1", r.getRazorpaySignature());
        assertNotNull(r.toString());
        r.hashCode();
    }

    @Test
    @DisplayName("SubscriptionStatusDto builder and getters")
    void subscriptionStatusDto() {
        Instant now = Instant.now();
        SubscriptionStatusDto dto = SubscriptionStatusDto.builder()
                .plan("PRO").status("ACTIVE").active(true)
                .startDate(now).endDate(now.plusSeconds(86400)).build();
        assertEquals("PRO", dto.getPlan());
        assertEquals("ACTIVE", dto.getStatus());
        assertTrue(dto.isActive());
        assertEquals(now, dto.getStartDate());
        assertNotNull(dto.toString());
    }
}
