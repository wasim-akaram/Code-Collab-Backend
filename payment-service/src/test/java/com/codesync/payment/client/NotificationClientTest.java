package com.codesync.payment.client;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link NotificationClient}.
 * Verifies that fire-and-forget notification methods handle errors gracefully
 * and don't throw exceptions that could interrupt payment processing.
 */
class NotificationClientTest {

    private final NotificationClient client = new NotificationClient();

    @Test
    @DisplayName("sendProActivatedNotification should not throw even when notification service is unavailable")
    void sendProActivated_serviceDown_shouldNotThrow() {
        // NotificationClient uses localhost:8088 — not running in test
        // This should log error but NOT throw
        assertDoesNotThrow(() -> client.sendProActivatedNotification("test@test.com"));
    }

    @Test
    @DisplayName("sendPaymentReceivedNotification should not throw even when notification service is unavailable")
    void sendPaymentReceived_serviceDown_shouldNotThrow() {
        assertDoesNotThrow(() -> client.sendPaymentReceivedNotification("test@test.com", 49900, "order_123"));
    }

    @Test
    @DisplayName("NotificationRequest DTO should get/set all fields")
    void notificationRequest_shouldSetAndGetFields() {
        NotificationClient.NotificationRequest req = new NotificationClient.NotificationRequest();
        req.setUserEmail("user@test.com");
        req.setActorEmail("actor@test.com");
        req.setTitle("Test Title");
        req.setType("TEST_TYPE");
        req.setMessage("Test message");
        req.setReferenceId(42L);
        req.setReferenceType("PROJECT");

        assertEquals("user@test.com", req.getUserEmail());
        assertEquals("actor@test.com", req.getActorEmail());
        assertEquals("Test Title", req.getTitle());
        assertEquals("TEST_TYPE", req.getType());
        assertEquals("Test message", req.getMessage());
        assertEquals(42L, req.getReferenceId());
        assertEquals("PROJECT", req.getReferenceType());
    }
}
