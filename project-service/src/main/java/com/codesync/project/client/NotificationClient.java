/*
 * Code reader note: Calls notification-service when project events should alert another user.
 */
package com.codesync.project.client;

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
 * HTTP client for calling notification-service from project-service.
 * Fires async, fire-and-forget notifications for fork and member-added events.
 * Failures are logged but never block the main project operation.
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
     * Notifies the original project owner that someone forked their project.
     */
    @Async
    public void sendForkNotification(String ownerEmail, String forkerEmail, String projectName) {
        try {
            log.info("[NOTIFY] Sending fork notification: owner={}, forker={}, project={}",
                    ownerEmail, forkerEmail, projectName);

            NotificationRequest req = new NotificationRequest();
            req.setUserEmail(ownerEmail);
            req.setActorEmail(forkerEmail);
            req.setTitle(String.format("%s forked \"%s\"", forkerEmail, projectName));
            req.setType("PROJECT_FORKED");
            req.setMessage(String.format("%s forked your project \"%s\". A copy has been created under their account.",
                    forkerEmail, projectName));
            req.setReferenceId(0L);
            req.setReferenceType("PROJECT");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<NotificationRequest> entity = new HttpEntity<>(req, headers);

            Object response = restTemplate.postForObject(NOTIFICATION_URL, entity, Object.class);
            log.info("[NOTIFY] Fork notification sent successfully to owner={}, response={}", ownerEmail, response);
        } catch (Exception e) {
            log.error("[NOTIFY] Failed to send fork notification to {}: {}", ownerEmail, e.getMessage(), e);
        }
    }

    /**
     * Notifies the new member that they were added to a project.
     */
    @Async
    public void sendMemberAddedNotification(String memberEmail, String ownerEmail,
                                             String projectName, String role) {
        try {
            log.info("[NOTIFY] Sending member-added notification: member={}, owner={}, project={}, role={}",
                    memberEmail, ownerEmail, projectName, role);

            NotificationRequest req = new NotificationRequest();
            req.setUserEmail(memberEmail);
            req.setActorEmail(ownerEmail);
            req.setTitle(String.format("%s added you to \"%s\"", ownerEmail, projectName));
            req.setType("PROJECT_MEMBER_ADDED");
            req.setMessage(String.format("%s added you to project \"%s\" as %s. You can now access this project from your dashboard.",
                    ownerEmail, projectName, role));
            req.setReferenceId(0L);
            req.setReferenceType("PROJECT");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<NotificationRequest> entity = new HttpEntity<>(req, headers);

            Object response = restTemplate.postForObject(NOTIFICATION_URL, entity, Object.class);
            log.info("[NOTIFY] Member-added notification sent successfully to {}, response={}", memberEmail, response);
        } catch (Exception e) {
            log.error("[NOTIFY] Failed to send member-added notification to {}: {}", memberEmail, e.getMessage(), e);
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
