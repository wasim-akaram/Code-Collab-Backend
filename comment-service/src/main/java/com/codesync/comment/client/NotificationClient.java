/*
 * Code reader note: Calls notification-service when comments mention another user.
 */
package com.codesync.comment.client;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP client for calling notification-service via Eureka load balancing.
 * Used to send @mention notifications when a user is tagged in a comment.
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
     * Sends an async @mention notification.
     * Fire-and-forget — failures are logged but do not break the comment creation flow.
     */
    @Async
    public void sendMentionNotification(String mentionedEmail, String authorEmail,
                                        Long commentId, Long fileId) {
        try {
            NotificationRequest req = new NotificationRequest();
            req.setUserEmail(mentionedEmail);
            req.setActorEmail(authorEmail);
            req.setTitle(authorEmail.split("@")[0] + " mentioned you");
            req.setType("COMMENT_MENTION");
            req.setMessage(String.format("%s mentioned you in a comment on file #%d",
                    authorEmail.split("@")[0], fileId));
            req.setReferenceId(commentId);
            req.setReferenceType("COMMENT");

            restTemplate.postForObject(NOTIFICATION_URL, req, Object.class);
            log.info("Mention notification sent to {} for comment #{}", mentionedEmail, commentId);
        } catch (Exception e) {
            // Non-blocking — log and move on so comment creation still succeeds
            log.warn("Failed to send mention notification to {}: {}", mentionedEmail, e.getMessage());
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
