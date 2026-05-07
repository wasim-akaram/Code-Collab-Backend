/*
 * Code reader note: Exposes REST endpoints for creating, listing, reading, counting, deleting, and emailing notifications.
 */
package com.codesync.notification.controller;

import com.codesync.notification.dto.NotificationDto;
import com.codesync.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for notification operations.
 *
 * X-User header (set by API Gateway from JWT sub) contains the user's email.
 *
 * Internal services (comment-service, collab-service, execution-service) call
 * POST /notifications/send without requiring X-User — recipient email is in the DTO body.
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Internal endpoint — used by other microservices to push a notification.
     * No X-User required; recipient email is embedded in the DTO.
     */
    @PostMapping("/send")
    public ResponseEntity<NotificationDto> sendNotification(@Valid @RequestBody NotificationDto dto) {
        return new ResponseEntity<>(notificationService.sendNotification(dto), HttpStatus.CREATED);
    }

    /**
     * Admin broadcast — sends the same notification to a list of recipients.
     * Body: { "recipientEmails": [...], "title": "...", "message": "...", "type": "BROADCAST" }
     */
    @PostMapping("/bulk")
    public ResponseEntity<Void> sendBulk(@RequestBody NotificationDto dto) {
        notificationService.sendBulk(
                dto.getRecipientEmails(),
                dto.getTitle(),
                dto.getMessage(),
                dto.getType()
        );
        return ResponseEntity.accepted().build();
    }

    /** Get all notifications for the authenticated user. */
    @GetMapping
    public ResponseEntity<List<NotificationDto>> getUserNotifications(
            @RequestHeader("X-User") String userEmail) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userEmail));
    }

    /** Get only unread notifications for the authenticated user. */
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDto>> getUnreadNotifications(
            @RequestHeader("X-User") String userEmail) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userEmail));
    }

    /** Get the count of unread notifications. */
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestHeader("X-User") String userEmail) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(userEmail)));
    }

    /** Mark a specific notification as read. */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<NotificationDto> markAsRead(
            @PathVariable Long notificationId,
            @RequestHeader("X-User") String userEmail) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId, userEmail));
    }

    /** Mark all notifications as read for the authenticated user. */
    @PutMapping("/read/all")
    public ResponseEntity<Void> markAllAsRead(@RequestHeader("X-User") String userEmail) {
        notificationService.markAllAsRead(userEmail);
        return ResponseEntity.noContent().build();
    }

    /** Delete a specific notification. */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long notificationId,
            @RequestHeader("X-User") String userEmail) {
        notificationService.deleteNotification(notificationId, userEmail);
        return ResponseEntity.noContent().build();
    }

    /** Delete all notifications for the authenticated user. */
    @DeleteMapping
    public ResponseEntity<Void> deleteAllNotifications(@RequestHeader("X-User") String userEmail) {
        notificationService.deleteAllNotifications(userEmail);
        return ResponseEntity.noContent().build();
    }

    /**
     * Fire-and-forget email alert (used internally for critical events).
     * Returns 202 Accepted since it runs asynchronously.
     */
    @PostMapping("/email")
    public ResponseEntity<Void> sendEmailAlert(@RequestBody Map<String, String> payload) {
        notificationService.sendEmailAlert(
                payload.get("toEmail"),
                payload.get("subject"),
                payload.get("body")
        );
        return ResponseEntity.accepted().build();
    }
}
