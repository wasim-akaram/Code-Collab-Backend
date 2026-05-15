package com.codesync.payment.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PaymentTransaction} entity.
 * Exercises Lombok-generated builder, getters, setters, equals, hashCode, toString.
 */
class PaymentTransactionTest {

    @Test
    @DisplayName("Builder should create PaymentTransaction with all fields")
    void builder_shouldPopulateAllFields() {
        PaymentTransaction txn = PaymentTransaction.builder()
                .id(1L)
                .userEmail("user@test.com")
                .razorpayOrderId("order_123")
                .razorpayPaymentId("pay_456")
                .razorpaySignature("sig_789")
                .amountPaise(49900)
                .currency("INR")
                .status("CREATED")
                .build();

        assertEquals(1L, txn.getId());
        assertEquals("user@test.com", txn.getUserEmail());
        assertEquals("order_123", txn.getRazorpayOrderId());
        assertEquals("pay_456", txn.getRazorpayPaymentId());
        assertEquals("sig_789", txn.getRazorpaySignature());
        assertEquals(49900, txn.getAmountPaise());
        assertEquals("INR", txn.getCurrency());
        assertEquals("CREATED", txn.getStatus());
    }

    @Test
    @DisplayName("Setters should update fields")
    void setters_shouldUpdateFields() {
        PaymentTransaction txn = new PaymentTransaction();
        txn.setId(2L);
        txn.setUserEmail("updated@test.com");
        txn.setRazorpayOrderId("order_new");
        txn.setRazorpayPaymentId("pay_new");
        txn.setRazorpaySignature("sig_new");
        txn.setAmountPaise(99900);
        txn.setCurrency("USD");
        txn.setStatus("SUCCESS");

        assertEquals(2L, txn.getId());
        assertEquals("updated@test.com", txn.getUserEmail());
        assertEquals("order_new", txn.getRazorpayOrderId());
        assertEquals("SUCCESS", txn.getStatus());
    }

    @Test
    @DisplayName("equals and hashCode should work correctly")
    void equalsAndHashCode_shouldWork() {
        PaymentTransaction a = PaymentTransaction.builder().id(1L).userEmail("a@test.com").build();
        PaymentTransaction b = PaymentTransaction.builder().id(1L).userEmail("a@test.com").build();
        PaymentTransaction c = PaymentTransaction.builder().id(2L).userEmail("b@test.com").build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    @DisplayName("toString should contain key fields")
    void toString_shouldContainFields() {
        PaymentTransaction txn = PaymentTransaction.builder()
                .id(1L).userEmail("user@test.com").status("SUCCESS").build();
        String str = txn.toString();
        assertTrue(str.contains("user@test.com"));
        assertTrue(str.contains("SUCCESS"));
    }
}
