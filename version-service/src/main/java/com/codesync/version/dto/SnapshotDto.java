/*
 * Code reader note: Transfers snapshot metadata/content through version-service API calls.
 */
package com.codesync.version.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Snapshot entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SnapshotDto {

    private Long snapshotId;

    @NotNull(message = "Project ID is required")
    private Long projectId;

    @NotNull(message = "File ID is required")
    private Long fileId;

    /** Numeric author ID (optional, populated server-side). */
    private Long authorId;

    @NotBlank(message = "Content cannot be empty")
    private String content;

    private String commitMessage;
    
    /** Email of the user who created this snapshot. */
    private String createdByEmail;

    /** SHA-256 hash of the content for integrity verification. */
    private String hash;

    /** Parent snapshot ID forming the history chain. */
    private Long parentSnapshotId;
    
    private String branchName;
    
    private String tag;
    
    private LocalDateTime createdAt;
}
