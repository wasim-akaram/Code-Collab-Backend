/*
 * Code reader note: Calls notification-service for collaboration events such as joins and kicks.
 */
package com.codesync.collab.client;

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
 * HTTP client for calling notification-service from collab-service.
 * Fires async, fire-and-forget notifications for session join and kick events.
 * Failures are logged but never block the main collab flow.
 */
@Slf4j
@Component
public class NotificationClient {

    // Dedicated RestTemplate with timeouts (not the @LoadBalanced bean)
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
     * Notifies a user that they have been added to / joined a collaboration session.
     */
    @Async
    public void sendSessionJoinNotification(String joinedEmail, String hostEmail, String sessionId) {
        try {
            log.info("[NOTIFY] Sending session-join notification: joined={}, host={}, session={}",
                    joinedEmail, hostEmail, sessionId);

            NotificationRequest req = new NotificationRequest();
            req.setUserEmail(joinedEmail);
            req.setActorEmail(hostEmail);
            req.setTitle(String.format("You joined %s's session", hostEmail));
            req.setType("SESSION_JOIN");
            req.setMessage(String.format("%s started a collaboration session (ID: %s). You are now collaborating in real time.",
                    hostEmail, sessionId));
            req.setReferenceId(0L);
            req.setReferenceType("SESSION");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<NotificationRequest> entity = new HttpEntity<>(req, headers);

            restTemplate.postForObject(NOTIFICATION_URL, entity, Object.class);
            log.info("[NOTIFY] Session-join notification sent to {}", joinedEmail);
        } catch (Exception e) {
            log.error("[NOTIFY] Failed to send session-join notification to {}: {}", joinedEmail, e.getMessage(), e);
        }
    }

    /**
     * Notifies a user that they were kicked from a collaboration session.
     */
    @Async
    public void sendSessionKickNotification(String kickedEmail, String hostEmail, String sessionId) {
        try {
            log.info("[NOTIFY] Sending session-kick notification: kicked={}, host={}, session={}",
                    kickedEmail, hostEmail, sessionId);

            NotificationRequest req = new NotificationRequest();
            req.setUserEmail(kickedEmail);
            req.setActorEmail(hostEmail);
            req.setTitle(String.format("Removed from %s's session", hostEmail));
            req.setType("SESSION_KICKED");
            req.setMessage(String.format("%s removed you from collaboration session (ID: %s).",
                    hostEmail, sessionId));
            req.setReferenceId(0L);
            req.setReferenceType("SESSION");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<NotificationRequest> entity = new HttpEntity<>(req, headers);

            restTemplate.postForObject(NOTIFICATION_URL, entity, Object.class);
            log.info("[NOTIFY] Session-kick notification sent to {}", kickedEmail);
        } catch (Exception e) {
            log.error("[NOTIFY] Failed to send session-kick notification to {}: {}", kickedEmail, e.getMessage(), e);
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

