/*
 * Code reader note: Calls notification-service when project events should alert another user.
 */
package com.codesync.project.client;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
public class NotificationClient {

    // Use a dedicated plain RestTemplate (not @LoadBalanced) so the URL is
    // resolved literally instead of through Eureka service discovery.
    private final RestTemplate restTemplate = new RestTemplate();

    // Direct URL — notification-service runs on port 8088
    private static final String NOTIFICATION_URL = "http://localhost:8088/notifications/send";

    /**
     * Notifies the original project owner that someone forked their project.
     */
    @Async
    public void sendForkNotification(String ownerEmail, String forkerEmail, String projectName) {
        try {
            NotificationRequest req = new NotificationRequest();
            req.setUserEmail(ownerEmail);
            req.setActorEmail(forkerEmail);
            req.setTitle(forkerEmail.split("@")[0] + " forked your project");
            req.setType("PROJECT_FORKED");
            req.setMessage(String.format("%s forked your project \"%s\".",
                    forkerEmail.split("@")[0], projectName));
            req.setReferenceId(0L);
            req.setReferenceType("PROJECT");

            restTemplate.postForObject(NOTIFICATION_URL, req, Object.class);
            log.info("Fork notification sent to project owner {}", ownerEmail);
        } catch (Exception e) {
            log.warn("Failed to send fork notification to {}: {}", ownerEmail, e.getMessage());
        }
    }

    /**
     * Notifies the new member that they were added to a project.
     */
    @Async
    public void sendMemberAddedNotification(String memberEmail, String ownerEmail,
                                             String projectName, String role) {
        try {
            NotificationRequest req = new NotificationRequest();
            req.setUserEmail(memberEmail);
            req.setActorEmail(ownerEmail);
            req.setTitle("You were added to a project");
            req.setType("PROJECT_MEMBER_ADDED");
            req.setMessage(String.format("%s added you to \"%s\" as %s.",
                    ownerEmail.split("@")[0], projectName, role));
            req.setReferenceId(0L);
            req.setReferenceType("PROJECT");

            restTemplate.postForObject(NOTIFICATION_URL, req, Object.class);
            log.info("Member-added notification sent to {}", memberEmail);
        } catch (Exception e) {
            log.warn("Failed to send member-added notification to {}: {}", memberEmail, e.getMessage());
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
