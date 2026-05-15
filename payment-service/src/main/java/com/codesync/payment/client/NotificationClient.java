package com.codesync.payment.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client for calling notification-service from payment-service.
 * Fires async, fire-and-forget notifications for payment events.
 * Failures are logged but never block the payment operation.
 */
@Slf4j
@Component
public class NotificationClient {

    private final RestTemplate restTemplate;

    // Direct URL — notification-service runs on port 8088
    private static final String NOTIFICATION_URL = "http://localhost:8088/notifications/send";

    public NotificationClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Notifies the user that their Pro subscription has been activated.
     */
    @Async
    public void sendProActivatedNotification(String userEmail) {
        try {
            log.info("[NOTIFY] Sending Pro activation notification to {}", userEmail);

            NotificationRequest req = new NotificationRequest();
            req.setUserEmail(userEmail);
            req.setActorEmail("system@codesync.live");
            req.setTitle("🎉 Welcome to CodeSync Pro!");
            req.setType("SUBSCRIPTION_ACTIVATED");
            req.setMessage("Your Pro subscription is now active! Enjoy unlimited projects, extended execution timeouts, and all premium features. Your plan renews in 30 days.");
            req.setReferenceId(0L);
            req.setReferenceType("SUBSCRIPTION");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<NotificationRequest> entity = new HttpEntity<>(req, headers);

            Object response = restTemplate.postForObject(NOTIFICATION_URL, entity, Object.class);
            log.info("[NOTIFY] Pro activation notification sent to {}, response={}", userEmail, response);
        } catch (Exception e) {
            log.error("[NOTIFY] Failed to send Pro activation notification to {}: {}", userEmail, e.getMessage(), e);
        }
    }

    /**
     * Notifies the user that a payment was received.
     */
    @Async
    public void sendPaymentReceivedNotification(String userEmail, int amountPaise, String orderId) {
        try {
            String amount = String.format("₹%d", amountPaise / 100);

            NotificationRequest req = new NotificationRequest();
            req.setUserEmail(userEmail);
            req.setActorEmail("system@codesync.live");
            req.setTitle("Payment received — " + amount);
            req.setType("PAYMENT_RECEIVED");
            req.setMessage(String.format("Payment of %s received successfully for CodeSync Pro (Order: %s). Thank you!", amount, orderId));
            req.setReferenceId(0L);
            req.setReferenceType("PAYMENT");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<NotificationRequest> entity = new HttpEntity<>(req, headers);

            restTemplate.postForObject(NOTIFICATION_URL, entity, Object.class);
            log.info("[NOTIFY] Payment notification sent to {}", userEmail);
        } catch (Exception e) {
            log.error("[NOTIFY] Failed to send payment notification to {}: {}", userEmail, e.getMessage(), e);
        }
    }

    @Data
    public static class NotificationRequest {
        private String userEmail;
        private String actorEmail;
        private String title;
        private String type;
        private String message;
        private Long referenceId;
        private String referenceType;
    }
}
