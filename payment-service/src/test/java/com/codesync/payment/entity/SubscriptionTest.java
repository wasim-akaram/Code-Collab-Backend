package com.codesync.payment.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Subscription} entity.
 */
class SubscriptionTest {

    @Test
    @DisplayName("Builder should create Subscription with all fields")
    void builder_shouldPopulateAllFields() {
        Instant now = Instant.now();
        Instant end = now.plusSeconds(86400 * 30);
        Subscription sub = Subscription.builder()
                .id(1L).userEmail("pro@test.com").plan("PRO").status("ACTIVE")
                .amountPaise(49900).startDate(now).endDate(end)
                .razorpayOrderId("order_1").razorpayPaymentId("pay_1").build();

        assertEquals(1L, sub.getId());
        assertEquals("pro@test.com", sub.getUserEmail());
        assertEquals("PRO", sub.getPlan());
        assertEquals("ACTIVE", sub.getStatus());
        assertEquals(49900, sub.getAmountPaise());
        assertEquals(now, sub.getStartDate());
        assertEquals(end, sub.getEndDate());
    }

    @Test
    @DisplayName("Setters should update fields")
    void setters_shouldUpdateFields() {
        Subscription sub = new Subscription();
        sub.setId(2L);
        sub.setUserEmail("user@test.com");
        sub.setPlan("FREE");
        sub.setStatus("EXPIRED");
        sub.setAmountPaise(0);
        sub.setStartDate(Instant.now());
        sub.setEndDate(Instant.now());
        sub.setRazorpayOrderId("o1");
        sub.setRazorpayPaymentId("p1");

        assertEquals(2L, sub.getId());
        assertEquals("EXPIRED", sub.getStatus());
        assertEquals("o1", sub.getRazorpayOrderId());
    }

    @Test
    @DisplayName("equals and hashCode should work correctly")
    void equalsAndHashCode() {
        Subscription a = Subscription.builder().id(1L).userEmail("a@t.com").plan("PRO").build();
        Subscription b = Subscription.builder().id(1L).userEmail("a@t.com").plan("PRO").build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, Subscription.builder().id(2L).build());
    }

    @Test
    @DisplayName("toString should contain key fields")
    void toString_shouldContainFields() {
        Subscription sub = Subscription.builder().userEmail("pro@t.com").plan("PRO").build();
        assertTrue(sub.toString().contains("PRO"));
    }
}
