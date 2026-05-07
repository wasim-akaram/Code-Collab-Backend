/*
 * Code reader note: Transfers comment thread data through comment-service API calls.
 */
package com.codesync.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Code Comment Entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {

    private Long commentId;

    @NotNull(message = "Project ID is required")
    private Long projectId;

    @NotNull(message = "File ID is required")
    private Long fileId;

    /** Email of the comment author. */
    private String authorEmail;

    @NotBlank(message = "Content cannot be blank")
    private String content;

    private Integer lineNumber;
    private Integer columnNumber;
    private Long parentCommentId;
    private Boolean resolved;
    private Long snapshotId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
