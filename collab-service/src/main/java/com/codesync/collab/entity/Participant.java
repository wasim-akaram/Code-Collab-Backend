/*
 * Code reader note: Represents a user currently or previously participating in a collaboration session.
 */
package com.codesync.collab.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a user participating in an active collaboration session.
 * Uses userEmail (Option A) to match the API Gateway's X-User header pattern.
 */
@Entity
@Table(name = "collab_participants", indexes = {
        @Index(name = "idx_participant_session", columnList = "sessionId"),
        @Index(name = "idx_participant_email", columnList = "userEmail")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long participantId;

    // UUID of the session being joined
    @Column(nullable = false)
    private String sessionId;

    // Numeric user ID — kept for DB compatibility; identity is email-based (userEmail).
    // Column exists in the schema; nullable so email-only inserts don't fail.
    @Column(name = "user_id", nullable = true)
    private Long userId;

    // Email of the user joining (from X-User gateway header)
    @Column(nullable = false)
    private String userEmail;

    // The user's role in the session (HOST, EDITOR, VIEWER)
    @Column(nullable = false)
    private String role;

    // Timestamp when they joined
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime joinedAt;

    // Timestamp when they left (null = still active)
    private LocalDateTime leftAt;

    // Current cursor line position for real-time tracking
    @Builder.Default
    private Integer cursorLine = 1;

    // Current cursor column position
    @Builder.Default
    private Integer cursorCol = 1;

    // Color assigned to the user's cursor for UI highlighting (e.g., "#FF5733")
    private String color;
}
