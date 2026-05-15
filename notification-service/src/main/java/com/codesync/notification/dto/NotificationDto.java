/*
 * Code reader note: Transfers notification data through notification-service API calls.
 */
package com.codesync.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Notifications.
 * Matches spec §4.8 entity fields: recipientId (userEmail), actorId (actorEmail),
 * title, message, relatedId, relatedType, isRead.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private Long notificationId;

    /** Email of the recipient. */
    @NotBlank(message = "Recipient email is required")
    private String userEmail;

    /** Email of the user who triggered this event (optional for system notifications). */
    private String actorEmail;

    /** Short notification header, e.g. "You were mentioned in file.js". */
    private String title;

    @NotBlank(message = "Type is required")
    private String type;

    @NotBlank(message = "Message is required")
    private String message;

    private Long referenceId;
    private String referenceType;
    private Boolean isRead;
    private LocalDateTime createdAt;

    // ─── Bulk broadcast ──────────────────────────────────────────────────────────

    /**
     * Used by the sendBulk endpoint only.
     * List of recipient emails for an admin broadcast.
     * Ignored for single-recipient notifications.
     */
    private List<String> recipientEmails;
}
