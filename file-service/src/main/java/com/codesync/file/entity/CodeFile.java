/*
 * Code reader note: Represents a persisted file or folder inside a project workspace.
 * Annotations used: @Entity and @Table map the class to the database table,
 * Lombok annotations generate constructors, accessors, and a builder,
 * @CreationTimestamp and @UpdateTimestamp manage timestamps automatically,
 * and JPA column annotations enforce constraints and soft-delete semantics.
 */
package com.codesync.file.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a file or folder inside a project.
 */
@Entity
@Table(name = "code_files", indexes = {
        @Index(name = "idx_project_id", columnList = "projectId"),
        @Index(name = "idx_file_path", columnList = "projectId, path")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_project_path_active",
                          columnNames = {"projectId", "path", "isDeleted"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fileId;

    /** ID of the project this file belongs to (references project-service). */
    @Column(nullable = false)
    private Long projectId;

    /** The name of the file (e.g., "App.java"). */
    @Column(nullable = false)
    private String name;

    /** The full path in the project tree (e.g., "src/main/App.java"). */
    @Column(nullable = false)
    private String path;

    /** The programming language or file type for syntax highlighting. */
    private String language;

    /** The actual text content of the file. Using TEXT for large strings. */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** Size of the file in bytes. */
    @Builder.Default
    private Long size = 0L;

    /** Email of the user who created this file (from X-User header). */
    @Column(nullable = false)
    private String createdBy;

    /** Email of the user who last edited the file content. */
    private String lastEditedBy;

    /** Timestamp when the file was created. */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** Timestamp when the file was last updated. */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** Soft delete flag to preserve data for restoration. */
    @Builder.Default
    @Column(nullable = false)
    private Boolean isDeleted = false;

    /** Flag to determine if this is a directory instead of a file. */
    @Builder.Default
    @Column(nullable = false)
    private Boolean isDirectory = false;
}
