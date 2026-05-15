/*
 * Code reader note: Provides database queries for execution jobs by job id, user, project, status, and language.
 */
package com.codesync.execution.repository;

import com.codesync.execution.entity.ExecutionJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing ExecutionJobs.
 */
@Repository
public interface ExecutionRepository extends JpaRepository<ExecutionJob, Long> {

    /** Retrieve a job by its UUID. */
    Optional<ExecutionJob> findByJobId(String jobId);

    /** Retrieve all executions triggered by a specific user (email). */
    List<ExecutionJob> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    /** Retrieve all executions for a specific project. */
    List<ExecutionJob> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    
    /** Find jobs stuck in running state for too long (for cleanup). */
    List<ExecutionJob> findByStatusAndCreatedAtBefore(String status, LocalDateTime threshold);
}
