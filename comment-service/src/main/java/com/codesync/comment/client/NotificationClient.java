/*
 * Code reader note: Calls notification-service when comments mention another user.
 */
package com.codesync.comment.client;

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
 * HTTP client for calling notification-service via direct HTTP.
 * Used to send @mention notifications when a user is tagged in a comment.
 */
@Slf4j
@Component
public class NotificationClient {

    // Dedicated RestTemplate with timeouts (not the @LoadBalanced bean)
    private final RestTemplate restTemplate;

    @org.springframework.beans.factory.annotation.Value("${NOTIFICATION_SERVICE_URL:http://localhost:8088}/notifications/send")
    private String notificationUrl;

    public NotificationClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Sends an async @mention notification.
     * Fire-and-forget — failures are logged but do not break the comment creation flow.
     */
    @Async
    public void sendMentionNotification(String mentionedEmail, String authorEmail,
                                        Long commentId, Long fileId) {
        try {
            log.info("[NOTIFY] Sending mention notification: mentioned={}, author={}, comment={}, file={}",
                    mentionedEmail, authorEmail, commentId, fileId);

            NotificationRequest req = new NotificationRequest();
            req.setUserEmail(mentionedEmail);
            req.setActorEmail(authorEmail);
            req.setTitle(String.format("%s mentioned you in a comment", authorEmail));
            req.setType("COMMENT_MENTION");
            req.setMessage(String.format("%s mentioned you in a comment on file #%d. Open the file to view the conversation.",
                    authorEmail, fileId));
            req.setReferenceId(commentId);
            req.setReferenceType("COMMENT");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<NotificationRequest> entity = new HttpEntity<>(req, headers);

            restTemplate.postForObject(notificationUrl, entity, Object.class);
            log.info("[NOTIFY] Mention notification sent to {} for comment #{}", mentionedEmail, commentId);
        } catch (Exception e) {
            // Non-blocking — log and move on so comment creation still succeeds
            log.error("[NOTIFY] Failed to send mention notification to {}: {}", mentionedEmail, e.getMessage(), e);
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
