/*
 * Code reader note: Calls notification-service for collaboration events such as joins and kicks.
 */
package com.codesync.collab.client;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client for calling notification-service from collab-service.
 * Fires async, fire-and-forget notifications for session join and kick events.
 * Failures are logged but never block the main collab flow.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationClient {

    // Use a dedicated plain RestTemplate (not @LoadBalanced) so the URL is
    // resolved literally instead of through Eureka service discovery.
    private final RestTemplate restTemplate = new RestTemplate();

    // Direct URL — notification-service runs on port 8088
    private static final String NOTIFICATION_URL = "http://localhost:8088/notifications/send";

    /**
     * Notifies a user that they have been added to / joined a collaboration session.
     */
    @Async
    public void sendSessionJoinNotification(String joinedEmail, String hostEmail, String sessionId) {
        try {
            NotificationRequest req = new NotificationRequest();
            req.setUserEmail(joinedEmail);
            req.setActorEmail(hostEmail);
            req.setTitle("You joined a collaboration session");
            req.setType("SESSION_JOIN");
            req.setMessage(String.format("%s started a session. You are now collaborating.",
                    hostEmail.split("@")[0]));
            req.setReferenceId(0L);
            req.setReferenceType("SESSION");

            restTemplate.postForObject(NOTIFICATION_URL, req, Object.class);
            log.info("Session-join notification sent to {}", joinedEmail);
        } catch (Exception e) {
            log.warn("Failed to send session-join notification to {}: {}", joinedEmail, e.getMessage());
        }
    }

    /**
     * Notifies a user that they were kicked from a collaboration session.
     */
    @Async
    public void sendSessionKickNotification(String kickedEmail, String hostEmail, String sessionId) {
        try {
            NotificationRequest req = new NotificationRequest();
            req.setUserEmail(kickedEmail);
            req.setActorEmail(hostEmail);
            req.setTitle("You were removed from a collaboration session");
            req.setType("SESSION_KICKED");
            req.setMessage(String.format("You were removed from the session by %s.",
                    hostEmail.split("@")[0]));
            req.setReferenceId(0L);
            req.setReferenceType("SESSION");

            restTemplate.postForObject(NOTIFICATION_URL, req, Object.class);
            log.info("Session-kick notification sent to {}", kickedEmail);
        } catch (Exception e) {
            log.warn("Failed to send session-kick notification to {}: {}", kickedEmail, e.getMessage());
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
