/*
 * Code reader note: Represents a persisted code comment or reply attached to a project/file/line.
 */
package com.codesync.comment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing an inline code comment or reply.
 */
@Entity
@Table(name = "comments", indexes = {
        @Index(name = "idx_comment_file", columnList = "fileId"),
        @Index(name = "idx_comment_project", columnList = "projectId"),
        @Index(name = "idx_comment_parent", columnList = "parentCommentId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentId;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private Long fileId;

    /** Email of the author (from X-User header). */
    @Column(nullable = false)
    private String authorEmail;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** Line number in the code where the comment is anchored. */
    private Integer lineNumber;

    /** Column number in the code. */
    private Integer columnNumber;

    /** Optional parent ID to support threading. */
    private Long parentCommentId;

    /** Marks whether a discussion thread is resolved. */
    @Builder.Default
    @Column(nullable = false)
    private Boolean resolved = false;

    /** The snapshot ID this comment is attached to. */
    private Long snapshotId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
