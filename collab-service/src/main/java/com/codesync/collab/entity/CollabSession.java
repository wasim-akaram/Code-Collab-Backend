/*
 * Code reader note: Represents a persisted live collaboration session for a project.
 * Annotations used: @Entity and @Table map the class to the database table,
 * Lombok annotations generate accessors, constructors, and a builder,
 * @CreationTimestamp fills createdAt automatically, and JPA column annotations
 * define uniqueness and nullability constraints.
 */
package com.codesync.collab.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing an active or ended real-time collaboration session.
 * Each session is tied to a specific file in a project.
 */
@Entity
@Table(name = "collab_sessions", indexes = {
        @Index(name = "idx_collab_session_id", columnList = "sessionId", unique = true),
        @Index(name = "idx_collab_project_id", columnList = "projectId"),
        @Index(name = "idx_collab_file_id", columnList = "fileId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollabSession {

    // Internal database ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // UUID exposed to clients to join the session
    @Column(nullable = false, unique = true)
    private String sessionId;

    // Associated project ID
    @Column(nullable = false)
    private Long projectId;

    // Associated file ID that is being edited
    @Column(nullable = false)
    private Long fileId;

    // The user ID of the session host/creator
    @Column(nullable = false)
    private Long ownerId;

    // Current status (e.g., ACTIVE, ENDED)
    @Column(nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    // Programming language being used
    private String language;

    // When the session started
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // When the session was terminated
    private LocalDateTime endedAt;

    // Maximum allowed participants in this session
    @Builder.Default
    private Integer maxParticipants = 10;

    // Flag indicating if a password is required to join
    @Builder.Default
    @Column(nullable = false)
    private Boolean isPasswordProtected = false;

    // Optional password required to join if isPasswordProtected is true
    private String sessionPassword;
}
