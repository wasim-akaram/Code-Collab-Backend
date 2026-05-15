/*
 * Code reader note: Defines snapshot and lightweight version-control operations implemented by the service layer.
 */
package com.codesync.version.service;

import com.codesync.version.dto.SnapshotDto;

import java.util.List;

/**
 * Interface defining the business logic operations for version control.
 * Follows the class diagram in Section 2.7 of the Code Collaboration spec.
 */
public interface VersionService {

    /** Create a new snapshot (commit) for a file. Hash and parent are auto-computed. */
    SnapshotDto createSnapshot(SnapshotDto dto, String userEmail);

    /** Get snapshot history for a file on a specific branch. */
    List<SnapshotDto> getFileHistory(Long fileId, String branchName);

    /** Get all snapshots for a file across ALL branches. */
    List<SnapshotDto> getSnapshotsByFile(Long fileId);

    /** Get all snapshots for a project across ALL branches. */
    List<SnapshotDto> getSnapshotsByProject(Long projectId);

    /** Get all snapshots for a project on a specific branch. */
    List<SnapshotDto> getSnapshotsByBranch(Long projectId, String branchName);

    /** Get a single snapshot by its ID. */
    SnapshotDto getSnapshotById(Long snapshotId);

    /** Get the latest snapshot for a file on a branch. */
    SnapshotDto getLatestSnapshot(Long fileId, String branchName);

    /**
     * Non-destructive restore: creates a NEW snapshot with the old content and a
     * "Restored from snapshot #X" commit message (per Section 2.7 requirement).
     */
    SnapshotDto restoreSnapshot(Long snapshotId, String userEmail);

    /** Get the unified diff text between two snapshot IDs. */
    String getDiff(Long oldSnapshotId, Long newSnapshotId);

    /** Tag a specific snapshot (e.g., "v1.0.0"). */
    SnapshotDto tagSnapshot(Long snapshotId, String tag);

    /**
     * Create a new branch by copying the latest snapshot of a file from the
     * source branch into the new branch name.
     */
    SnapshotDto createBranch(Long fileId, String sourceBranch, String newBranch, String userEmail);

    /** List all distinct branch names that exist for a project. */
    List<String> getBranches(Long projectId);
}
