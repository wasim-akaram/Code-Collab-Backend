/*
 * Code reader note: Provides database queries for collaboration sessions by id, project, active state, and last activity.
 */
package com.codesync.collab.repository;

import com.codesync.collab.entity.CollabSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing collaboration sessions.
 */
@Repository
public interface CollabRepository extends JpaRepository<CollabSession, Long> {

    // Find a session by its public UUID
    Optional<CollabSession> findBySessionId(String sessionId);

    // Find all sessions associated with a specific project
    List<CollabSession> findByProjectId(Long projectId);

    // Find all sessions active on a specific file
    List<CollabSession> findByFileId(Long fileId);

    // Find all active sessions for a project
    List<CollabSession> findByProjectIdAndStatus(Long projectId, String status);
}
