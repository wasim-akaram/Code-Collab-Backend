/*
 * Code reader note: Transfers execution job input and result data through the execution API.
 */
package com.codesync.execution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Execution Job.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionJobDto {

    private String jobId;

    @NotNull(message = "Project ID is required")
    private Long projectId;

    private Long fileId;

    /** Email of the user who triggered the execution. */
    private String userEmail;

    @NotBlank(message = "Language is required")
    private String language;

    @NotBlank(message = "Source code cannot be empty")
    private String sourceCode;

    private String stdin;

    private String status;
    private String stdout;
    private String stderr;
    private Integer exitCode;
    private Long executionTimeMs;
    private Long memoryUsedKb;
    
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
