/*
 * Code reader note: Provides database queries for snapshots by file, project, branch, tag, author, and created time.
 */
package com.codesync.version.repository;

import com.codesync.version.entity.Snapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Snapshots in the database.
 * Provides all query methods required by the Version/Snapshot-Service class diagram.
 */
@Repository
public interface SnapshotRepository extends JpaRepository<Snapshot, Long> {

    // ─── File-scoped queries ─────────────────────────────────────────────────

    /** All snapshots for a file on a specific branch, newest first. */
    List<Snapshot> findByFileIdAndBranchNameOrderByCreatedAtDesc(Long fileId, String branchName);

    /** All snapshots for a file across ALL branches, newest first. */
    List<Snapshot> findByFileIdOrderByCreatedAtDesc(Long fileId);

    /** Latest snapshot for a file on a branch. */
    Optional<Snapshot> findFirstByFileIdAndBranchNameOrderByCreatedAtDesc(Long fileId, String branchName);

    // ─── Project-scoped queries ──────────────────────────────────────────────

    /** All snapshots for a project on a specific branch, newest first. */
    List<Snapshot> findByProjectIdAndBranchNameOrderByCreatedAtDesc(Long projectId, String branchName);

    /** All snapshots for a project across ALL branches, newest first. */
    List<Snapshot> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    /** Find snapshots by tag within a project. */
    List<Snapshot> findByProjectIdAndTag(Long projectId, String tag);

    /** List all distinct branch names for a project. */
    @Query("SELECT DISTINCT s.branchName FROM Snapshot s WHERE s.projectId = :projectId ORDER BY s.branchName")
    List<String> findDistinctBranchNamesByProjectId(@Param("projectId") Long projectId);

    // ─── Author queries ──────────────────────────────────────────────────────

    /** Find snapshots by author email. */
    List<Snapshot> findByCreatedByEmail(String email);

    // ─── Integrity / lookup queries ──────────────────────────────────────────

    /** Find a snapshot by its SHA-256 content hash. */
    Optional<Snapshot> findByHash(String hash);

    /** Find a snapshot by tag (global lookup). */
    Optional<Snapshot> findByTag(String tag);
}
