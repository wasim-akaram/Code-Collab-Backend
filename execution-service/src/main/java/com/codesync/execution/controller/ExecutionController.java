/*
 * Code reader note: Exposes REST endpoints for submitting executions, reading job history/results, cancellation, language metadata, and stats.
 */
package com.codesync.execution.controller;

import com.codesync.execution.dto.ExecutionJobDto;
import com.codesync.execution.service.ExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Code Execution operations.
 * X-User header contains the user's email (set by the API Gateway).
 */
@RestController
@RequestMapping("/executions")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionService executionService;

    /** Submit a new code execution job. */
    @PostMapping
    public ResponseEntity<ExecutionJobDto> submitExecution(@Valid @RequestBody ExecutionJobDto jobDto,
                                                           @RequestHeader("X-User") String userEmail) {
        return new ResponseEntity<>(executionService.submitExecution(jobDto, userEmail), HttpStatus.CREATED);
    }

    /** Get job by its UUID. */
    @GetMapping("/{jobId}")
    public ResponseEntity<ExecutionJobDto> getJobById(@PathVariable String jobId) {
        return ResponseEntity.ok(executionService.getJobById(jobId));
    }

    /** Get all executions by the current user. */
    @GetMapping("/user")
    public ResponseEntity<List<ExecutionJobDto>> getExecutionsByUser(@RequestHeader("X-User") String userEmail) {
        return ResponseEntity.ok(executionService.getExecutionsByUser(userEmail));
    }

    /** Get all executions for a specific project. */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ExecutionJobDto>> getExecutionsByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(executionService.getExecutionsByProject(projectId));
    }

    /** Cancel an ongoing execution. */
    @PostMapping("/{jobId}/cancel")
    public ResponseEntity<Void> cancelExecution(@PathVariable String jobId,
                                                @RequestHeader("X-User") String userEmail) {
        executionService.cancelExecution(jobId, userEmail);
        return ResponseEntity.ok().build();
    }

    /** Get execution result (poll this until status is COMPLETED/FAILED). */
    @GetMapping("/{jobId}/result")
    public ResponseEntity<ExecutionJobDto> getExecutionResult(@PathVariable String jobId) {
        return ResponseEntity.ok(executionService.getExecutionResult(jobId));
    }

    /** Get supported programming languages. */
    @GetMapping("/languages")
    public ResponseEntity<List<String>> getSupportedLanguages() {
        return ResponseEntity.ok(executionService.getSupportedLanguages());
    }

    /** Get version of a supported language runtime. */
    @GetMapping("/languages/{language}/version")
    public ResponseEntity<Map<String, String>> getLanguageVersion(@PathVariable String language) {
        return ResponseEntity.ok(Map.of("language", language, "version", executionService.getLanguageVersion(language)));
    }

    /** Get execution statistics for a project. */
    @GetMapping("/stats/project/{projectId}")
    public ResponseEntity<Map<String, Object>> getExecutionStats(@PathVariable Long projectId) {
        return ResponseEntity.ok(executionService.getExecutionStats(projectId));
    }

    // ─── Admin-only endpoints ─────────────────────────────────────────────────

    /** Get ALL execution jobs across all users (admin only). */
    @GetMapping("/admin/all")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ExecutionJobDto>> getAllExecutionsAdmin() {
        return ResponseEntity.ok(executionService.getAllExecutionsAdmin());
    }

    /** Get platform-wide execution stats (admin only). */
    @GetMapping("/admin/stats")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getPlatformStats() {
        return ResponseEntity.ok(executionService.getPlatformStats());
    }
}
