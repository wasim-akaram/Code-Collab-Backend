/*
 * Code reader note: Transfers file/folder metadata and content between the file API and clients.
 */
package com.codesync.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for CodeFile entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeFileDto {

    private Long fileId;

    @NotNull(message = "Project ID is required")
    private Long projectId;

    @NotBlank(message = "File name cannot be blank")
    private String name;

    @NotBlank(message = "File path cannot be blank")
    private String path;

    /** Language for syntax highlighting (can be null for folders). */
    private String language;

    /** File content (text). */
    private String content;

    /** File size in bytes. */
    private Long size;

    /** Is it a directory or a file? */
    private Boolean isDirectory;

    /** Email of the creator. */
    private String createdBy;

    /** Email of last editor. */
    private String lastEditedBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
