/*
 * Code reader note: Implements job creation, async sandbox execution flow, status transitions, cancellation, results, language data, and statistics.
 */
package com.codesync.execution.service;

import com.codesync.execution.dto.ExecutionJobDto;
import com.codesync.execution.entity.ExecutionJob;
import com.codesync.execution.exception.ResourceNotFoundException;
import com.codesync.execution.repository.ExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Implementation of ExecutionService.
 * Uses the Piston API (https://emkc.org/api/v2/piston) to execute code in a sandbox.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionServiceImpl implements ExecutionService {

    private final ExecutionRepository executionRepository;
    private final org.springframework.web.client.RestTemplate restTemplate;

    @Override
    @Transactional
    public ExecutionJobDto submitExecution(ExecutionJobDto dto, String userEmail) {
        // The client gets a stable UUID jobId immediately, while the database id
        // remains an internal persistence detail.
        ExecutionJob job = ExecutionJob.builder()
                .jobId(UUID.randomUUID().toString())
                .projectId(dto.getProjectId())
                .fileId(dto.getFileId())
                .userEmail(userEmail)
                .language(dto.getLanguage())
                .sourceCode(dto.getSourceCode())
                .stdin(dto.getStdin())
                .status("QUEUED")
                .build();
                
        ExecutionJob saved = executionRepository.save(job);
        
        // Trigger async execution
        // The HTTP request returns after queuing; execution continues in a
        // background @Async method.
        executeInSandbox(saved.getJobId());
        
        return mapToDto(saved);
    }

    @Override
    public ExecutionJobDto getJobById(String jobId) {
        return mapToDto(executionRepository.findByJobId(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found")));
    }

    @Override
    public List<ExecutionJobDto> getExecutionsByUser(String userEmail) {
        return executionRepository.findByUserEmailOrderByCreatedAtDesc(userEmail).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExecutionJobDto> getExecutionsByProject(Long projectId) {
        return executionRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelExecution(String jobId, String userEmail) {
        ExecutionJob job = executionRepository.findByJobId(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
                
        if ("QUEUED".equals(job.getStatus()) || "RUNNING".equals(job.getStatus())) {
            job.setStatus("CANCELLED");
            job.setCompletedAt(LocalDateTime.now());
            executionRepository.save(job);
        }
    }

    @Override
    public ExecutionJobDto getExecutionResult(String jobId) {
        return getJobById(jobId);
    }

    @Override
    public List<String> getSupportedLanguages() {
        return Arrays.asList("java", "python", "javascript", "typescript", "cpp", "c", "go", "rust", "ruby", "php");
    }

    @Override
    public String getLanguageVersion(String language) {
        return switch (language.toLowerCase()) {
            case "java" -> "15.0.2";
            case "python" -> "3.10.0";
            case "javascript" -> "18.15.0";
            case "typescript" -> "5.0.3";
            case "cpp", "c" -> "10.2.0";
            case "go" -> "1.16.2";
            case "rust" -> "1.68.2";
            case "ruby" -> "3.0.1";
            case "php" -> "8.2.3";
            default -> "Unknown";
        };
    }

    @Override
    public Map<String, Object> getExecutionStats(Long projectId) {
        long total = executionRepository.findByProjectIdOrderByCreatedAtDesc(projectId).size();
        return Map.of(
            "totalExecutions", total,
            "projectId", projectId
        );
    }

    // ─── LOCAL SANDBOX EXECUTION ──────────────────────────────────────────

    @Async
    public CompletableFuture<Void> executeInSandbox(String jobId) {
        ExecutionJob job = executionRepository.findByJobId(jobId).orElse(null);
        // If the job was cancelled or deleted before the async worker started,
        // there is nothing left to execute.
        if (job == null || "CANCELLED".equals(job.getStatus())) return CompletableFuture.completedFuture(null);
        
        try {
            job.setStatus("RUNNING");
            executionRepository.save(job);
            
            long startTime = System.currentTimeMillis();
            
            // Create temp file for the source code
            // Each run gets an isolated temp directory so generated class files or
            // compiled binaries do not collide with another user's run.
            java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("codesync-exec-");
            String fileName = getFileName(job.getLanguage());
            java.nio.file.Path sourceFile = tempDir.resolve(fileName);
            java.nio.file.Files.writeString(sourceFile, job.getSourceCode());
            
            // Build the command based on language
            ProcessBuilder pb = createProcessBuilder(job.getLanguage(), sourceFile, tempDir);
            if (pb == null) {
                // Unsupported languages fail the job record cleanly instead of
                // throwing an exception back out of the async worker.
                job.setStatus("FAILED");
                job.setStderr("Unsupported language: " + job.getLanguage());
                job.setExitCode(-1);
                job.setCompletedAt(LocalDateTime.now());
                executionRepository.save(job);
                cleanup(tempDir);
                return CompletableFuture.completedFuture(null);
            }
            
            pb.redirectErrorStream(false);
            pb.directory(tempDir.toFile());
            
            Process process = pb.start();
            
            // Feed stdin if provided
            if (job.getStdin() != null && !job.getStdin().isEmpty()) {
                try (var os = process.getOutputStream()) {
                    os.write(job.getStdin().getBytes());
                    os.flush();
                }
            } else {
                process.getOutputStream().close();
            }
            
            // Read stdout and stderr concurrently
            // These reads capture what the program wrote so the frontend can show
            // terminal-like output after the process exits.
            String stdout = new String(process.getInputStream().readAllBytes());
            String stderr = new String(process.getErrorStream().readAllBytes());
            
            boolean finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
            long endTime = System.currentTimeMillis();
            
            // Re-fetch in case of cancellation
            // The user may cancel from another request while the process is running.
            // Re-reading the row prevents a late worker from overwriting CANCELLED.
            job = executionRepository.findByJobId(jobId).orElse(null);
            if (job == null || "CANCELLED".equals(job.getStatus())) {
                process.destroyForcibly();
                cleanup(tempDir);
                return CompletableFuture.completedFuture(null);
            }
            
            if (!finished) {
                // A hard timeout protects the server from code that never exits.
                process.destroyForcibly();
                job.setStatus("TIMED_OUT");
                job.setStdout(stdout);
                job.setStderr("Execution timed out after 30 seconds");
                job.setExitCode(-1);
            } else {
                job.setStatus("COMPLETED");
                job.setStdout(stdout);
                job.setStderr(stderr);
                job.setExitCode(process.exitValue());
            }
            
            job.setExecutionTimeMs(endTime - startTime);
            job.setMemoryUsedKb(0L);
            job.setCompletedAt(LocalDateTime.now());
            executionRepository.save(job);
            
            cleanup(tempDir);
            
        } catch (Exception e) {
            log.error("Execution failed for job {}: {}", jobId, e.getMessage());
            job = executionRepository.findByJobId(jobId).orElse(null);
            if (job != null && !"CANCELLED".equals(job.getStatus())) {
                job.setStatus("FAILED");
                job.setStderr("Execution error: " + e.getMessage());
                job.setExitCode(-1);
                job.setCompletedAt(LocalDateTime.now());
                executionRepository.save(job);
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    private ProcessBuilder createProcessBuilder(String language, java.nio.file.Path sourceFile, java.nio.file.Path tempDir) {
        String os = System.getProperty("os.name").toLowerCase();
        // Compiled languages use shell commands because they need two steps:
        // compile first, then run the generated output.
        boolean isWindows = os.contains("win");
        
        return switch (language.toLowerCase()) {
            case "python" -> new ProcessBuilder("python", sourceFile.toString());
            case "javascript" -> new ProcessBuilder("node", sourceFile.toString());
            case "java" -> {
                // Compile then run
                String className = "Main";
                yield new ProcessBuilder(isWindows ? "cmd" : "sh",
                        isWindows ? "/c" : "-c",
                        "javac " + sourceFile.getFileName() + " && java -cp . " + className);
            }
            case "typescript" -> new ProcessBuilder(isWindows ? "cmd" : "sh",
                    isWindows ? "/c" : "-c",
                    "npx -y ts-node " + sourceFile.toString());
            case "cpp" -> new ProcessBuilder(isWindows ? "cmd" : "sh",
                    isWindows ? "/c" : "-c",
                    "g++ -o program " + sourceFile.getFileName() + " && " + (isWindows ? "program.exe" : "./program"));
            case "c" -> new ProcessBuilder(isWindows ? "cmd" : "sh",
                    isWindows ? "/c" : "-c",
                    "gcc -o program " + sourceFile.getFileName() + " && " + (isWindows ? "program.exe" : "./program"));
            case "go" -> new ProcessBuilder("go", "run", sourceFile.toString());
            default -> null;
        };
    }

    private String getFileName(String language) {
        return switch (language.toLowerCase()) {
            case "python" -> "main.py";
            case "javascript" -> "main.js";
            case "typescript" -> "main.ts";
            case "java" -> "Main.java";
            case "cpp" -> "main.cpp";
            case "c" -> "main.c";
            case "go" -> "main.go";
            case "rust" -> "main.rs";
            case "ruby" -> "main.rb";
            case "php" -> "main.php";
            default -> "main.txt";
        };
    }

    private void cleanup(java.nio.file.Path tempDir) {
        try {
            // Delete children before parents; reverseOrder is what makes that safe
            // for nested directories.
            java.nio.file.Files.walk(tempDir)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> {
                    try { java.nio.file.Files.deleteIfExists(p); } catch (Exception ignored) {}
                });
        } catch (Exception ignored) {}
    }
    
    private ExecutionJobDto mapToDto(ExecutionJob job) {
        return ExecutionJobDto.builder()
                .jobId(job.getJobId())
                .projectId(job.getProjectId())
                .fileId(job.getFileId())
                .userEmail(job.getUserEmail())
                .language(job.getLanguage())
                .sourceCode(job.getSourceCode())
                .stdin(job.getStdin())
                .status(job.getStatus())
                .stdout(job.getStdout())
                .stderr(job.getStderr())
                .exitCode(job.getExitCode())
                .executionTimeMs(job.getExecutionTimeMs())
                .memoryUsedKb(job.getMemoryUsedKb())
                .createdAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }

    // ─── Admin-only operations ────────────────────────────────────────────────

    @Override
    public List<ExecutionJobDto> getAllExecutionsAdmin() {
        return executionRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getPlatformStats() {
        List<ExecutionJob> all = executionRepository.findAll();
        long total     = all.size();
        long running   = all.stream().filter(j -> "RUNNING".equals(j.getStatus())).count();
        long completed = all.stream().filter(j -> "COMPLETED".equals(j.getStatus())).count();
        long failed    = all.stream().filter(j -> "FAILED".equals(j.getStatus())).count();
        long cancelled = all.stream().filter(j -> "CANCELLED".equals(j.getStatus())).count();
        return Map.of(
                "totalExecutions", total,
                "running",         running,
                "completed",       completed,
                "failed",          failed,
                "cancelled",       cancelled
        );
    }
}
