/*
 * Code reader note: Represents a persisted in-app notification for a user.
 */
package com.codesync.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing an alert/notification sent to a user.
 *
 * Fields match the spec diagram (§4.8):
 *   notificationId, recipientId (userEmail), actorId (actorEmail), type,
 *   title, message, relatedId, relatedType, isRead, createdAt
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_email", columnList = "userEmail"),
        @Index(name = "idx_notification_read",  columnList = "isRead")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    /** Email of the user receiving the notification (recipientId in spec). */
    @Column(nullable = false)
    private String userEmail;

    /**
     * Email of the user who triggered the event (actorId in spec).
     * E.g., the person who wrote the comment or started the session.
     * Nullable — system-generated notifications have no actor.
     */
    @Column
    private String actorEmail;

    /**
     * Short notification header, e.g. "New mention in file.js" (title in spec).
     */
    @Column(length = 200)
    private String title;

    /** Notification category — drives the icon and routing logic in the UI. */
    @Column(nullable = false)
    private String type; // COMMENT_MENTION | SESSION_INVITE | EXECUTION_COMPLETE | PROJECT_INVITE | SNAPSHOT

    /** Full human-readable message displayed in the notification panel. */
    @Column(nullable = false, length = 500)
    private String message;

    /** ID of the related resource (commentId, sessionId, snapshotId, projectId). */
    private Long referenceId;

    /** Type of the related resource (COMMENT, SESSION, SNAPSHOT, PROJECT, EXECUTION). */
    private String referenceType;

    /** Whether the user has read this notification. */
    @Builder.Default
    @Column(nullable = false)
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
