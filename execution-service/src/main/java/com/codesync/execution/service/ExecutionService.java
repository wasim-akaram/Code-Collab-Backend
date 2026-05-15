/*
 * Code reader note: Defines code execution job operations implemented by the service layer.
 */
package com.codesync.execution.service;

import com.codesync.execution.dto.ExecutionJobDto;

import java.util.List;
import java.util.Map;

/**
 * Interface defining code execution operations.
 */
public interface ExecutionService {

    /** Submits code for execution asynchronously. */
    ExecutionJobDto submitExecution(ExecutionJobDto dto, String userEmail);

    /** Retrieves a specific job by its UUID. */
    ExecutionJobDto getJobById(String jobId);

    /** Retrieves executions by a user (email). */
    List<ExecutionJobDto> getExecutionsByUser(String userEmail);

    /** Retrieves executions for a project. */
    List<ExecutionJobDto> getExecutionsByProject(Long projectId);

    /** Cancels an ongoing execution. */
    void cancelExecution(String jobId, String userEmail);

    /** Get the final result of an execution. */
    ExecutionJobDto getExecutionResult(String jobId);

    /** Get list of supported languages. */
    List<String> getSupportedLanguages();

    /** Get specific version of a language compiler/runtime. */
    String getLanguageVersion(String language);

    /** Get execution statistics for a project. */
    Map<String, Object> getExecutionStats(Long projectId);

    // ─── Admin-only operations ────────────────────────────────────────────────

    /** Returns ALL execution jobs across all users (admin only). */
    List<ExecutionJobDto> getAllExecutionsAdmin();

    /** Returns platform-wide execution stats (admin only). */
    Map<String, Object> getPlatformStats();
}
