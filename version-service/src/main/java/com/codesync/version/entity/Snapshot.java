/*
 * Code reader note: Represents a stored version snapshot of a file in a project and branch.
 */
package com.codesync.version.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Entity representing a point-in-time snapshot of a file (similar to a Git commit).
 * Each snapshot captures the full file content with a SHA-256 hash for integrity,
 * a commit message, a parent snapshot ID forming the history chain, and a branch label.
 */
@Entity
@Table(name = "snapshots", indexes = {
        @Index(name = "idx_snapshot_project", columnList = "projectId"),
        @Index(name = "idx_snapshot_file", columnList = "fileId"),
        @Index(name = "idx_snapshot_branch", columnList = "branchName"),
        @Index(name = "idx_snapshot_hash", columnList = "hash")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Snapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long snapshotId;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long fileId;

    /** Numeric author ID for cross-service reference. */
    private Long authorId;

    /** The text content of the file at this snapshot. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** A description of what was changed (like a commit message). */
    private String commitMessage;

    /** Email of the user who created this snapshot (from X-User header). */
    @Column(nullable = false)
    private String createdByEmail;

    /** SHA-256 hash of the content for integrity verification. */
    @Column(length = 64)
    private String hash;

    /** The ID of the previous snapshot in the history chain (forms a DAG). */
    private Long parentSnapshotId;

    /** The branch name this snapshot belongs to (default 'main'). */
    @Builder.Default
    @Column(nullable = false)
    private String branchName = "main";

    /** Optional tag (e.g., 'v1.0.0') for releases. */
    private String tag;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
