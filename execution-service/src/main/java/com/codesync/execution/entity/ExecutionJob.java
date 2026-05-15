/*
 * Code reader note: Represents a persisted code execution request, status, output, errors, and timing data.
 */
package com.codesync.execution.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a single code execution request and its result.
 */
@Entity
@Table(name = "execution_jobs", indexes = {
        @Index(name = "idx_exec_job_id", columnList = "jobId", unique = true),
        @Index(name = "idx_exec_project", columnList = "projectId"),
        @Index(name = "idx_exec_user", columnList = "userEmail")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID for tracking the job asynchronously. */
    @Column(nullable = false, unique = true)
    private String jobId;

    /** Associated project. */
    @Column(nullable = false)
    private Long projectId;

    /** Associated file (if running a specific file). */
    private Long fileId;

    /** Email of the user who triggered the execution (from X-User header). */
    @Column(nullable = false)
    private String userEmail;

    /** Programming language (e.g., java, python, javascript). */
    @Column(nullable = false)
    private String language;

    /** The actual code sent for execution. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String sourceCode;

    /** Original file name (e.g. Calculator.java). */
    private String fileName;

    /** Optional standard input provided by the user. */
    @Column(columnDefinition = "TEXT")
    private String stdin;

    /** Current status (QUEUED, RUNNING, COMPLETED, FAILED, TIMED_OUT, CANCELLED). */
    @Column(nullable = false)
    @Builder.Default
    private String status = "QUEUED";

    /** Standard output from the execution. */
    @Column(columnDefinition = "TEXT")
    private String stdout;

    /** Standard error from the execution. */
    @Column(columnDefinition = "TEXT")
    private String stderr;

    /** Process exit code. */
    private Integer exitCode;

    /** Total execution time in milliseconds. */
    private Long executionTimeMs;

    /** Total memory used by the sandbox in KB. */
    private Long memoryUsedKb;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** Timestamp when execution finished. */
    private LocalDateTime completedAt;
}
