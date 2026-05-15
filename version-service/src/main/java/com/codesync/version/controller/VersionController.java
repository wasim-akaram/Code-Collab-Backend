/*
 * Code reader note: Exposes REST endpoints for snapshots, file and project history,
 * latest versions, restore, tags, diffs, and branches.
 * Annotations used: @RestController publishes the API, @RequestMapping sets the
 * /versions base path, @RequiredArgsConstructor injects the service, and the
 * mapping annotations define the HTTP routes. @Valid enforces request validation.
 */
package com.codesync.version.controller;

import com.codesync.version.dto.SnapshotDto;
import com.codesync.version.service.VersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for snapshot and versioning operations.
 * All endpoints sit behind the API Gateway which injects the authenticated
 * user's email via the {@code X-User} header.
 *
 * Endpoints (all prefixed with /versions):
 *   POST   /snapshots                            – create snapshot
 *   GET    /snapshots/{id}                       – get by ID
 *   GET    /files/{fileId}/history?branch=main   – file history on a branch
 *   GET    /files/{fileId}/snapshots             – file history ALL branches
 *   GET    /files/{fileId}/latest?branch=main    – latest snapshot for file
 *   GET    /diff?oldId=X&newId=Y                 – unified diff
 *   POST   /snapshots/{id}/tag                   – tag a snapshot
 *   POST   /snapshots/{id}/restore               – non-destructive restore
 *   GET    /projects/{projectId}/snapshots       – all project snapshots
 *   GET    /projects/{projectId}/branches        – list branch names
 *   POST   /branches                             – create a new branch
 */
@RestController
@RequestMapping("/versions")
@RequiredArgsConstructor
public class VersionController {

    private final VersionService versionService;

    // ─── Snapshot CRUD ────────────────────────────────────────────────────────

    /** Create a new snapshot (commit). */
    @PostMapping("/snapshots")
    public ResponseEntity<SnapshotDto> createSnapshot(
            @Valid @RequestBody SnapshotDto snapshotDto,
            @RequestHeader("X-User") String userEmail) {
        return new ResponseEntity<>(versionService.createSnapshot(snapshotDto, userEmail), HttpStatus.CREATED);
    }

    /** Get a snapshot by ID. */
    @GetMapping("/snapshots/{id}")
    public ResponseEntity<SnapshotDto> getSnapshot(@PathVariable Long id) {
        return ResponseEntity.ok(versionService.getSnapshotById(id));
    }

    /** Tag a snapshot (e.g. "v1.0.0"). */
    @PostMapping("/snapshots/{id}/tag")
    public ResponseEntity<SnapshotDto> tagSnapshot(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(versionService.tagSnapshot(id, body.get("tag")));
    }

    /**
     * Non-destructive restore: creates a NEW snapshot with the content from
     * the specified snapshot (per Section 2.7 of the spec).
     */
    @PostMapping("/snapshots/{id}/restore")
    public ResponseEntity<SnapshotDto> restoreSnapshot(
            @PathVariable Long id,
            @RequestHeader("X-User") String userEmail) {
        return new ResponseEntity<>(versionService.restoreSnapshot(id, userEmail), HttpStatus.CREATED);
    }

    // ─── File-scoped History ──────────────────────────────────────────────────

    /** Get snapshot history for a file on a specific branch (default: main). */
    @GetMapping("/files/{fileId}/history")
    public ResponseEntity<List<SnapshotDto>> getFileHistory(
            @PathVariable Long fileId,
            @RequestParam(defaultValue = "main") String branch) {
        return ResponseEntity.ok(versionService.getFileHistory(fileId, branch));
    }

    /** Get all snapshots for a file across ALL branches. */
    @GetMapping("/files/{fileId}/snapshots")
    public ResponseEntity<List<SnapshotDto>> getSnapshotsByFile(@PathVariable Long fileId) {
        return ResponseEntity.ok(versionService.getSnapshotsByFile(fileId));
    }

    /** Get the latest snapshot for a file on a branch. */
    @GetMapping("/files/{fileId}/latest")
    public ResponseEntity<SnapshotDto> getLatestSnapshot(
            @PathVariable Long fileId,
            @RequestParam(defaultValue = "main") String branch) {
        return ResponseEntity.ok(versionService.getLatestSnapshot(fileId, branch));
    }

    // ─── Project-scoped Queries ───────────────────────────────────────────────

    /** Get all snapshots for a project across ALL branches, newest first. */
    @GetMapping("/projects/{projectId}/snapshots")
    public ResponseEntity<List<SnapshotDto>> getProjectSnapshots(@PathVariable Long projectId) {
        return ResponseEntity.ok(versionService.getSnapshotsByProject(projectId));
    }

    /** List all distinct branch names that exist for a project. */
    @GetMapping("/projects/{projectId}/branches")
    public ResponseEntity<List<String>> getBranches(@PathVariable Long projectId) {
        return ResponseEntity.ok(versionService.getBranches(projectId));
    }

    // ─── Diff ─────────────────────────────────────────────────────────────────

    /** Compute the unified diff between two snapshots. */
    @GetMapping("/diff")
    public ResponseEntity<Map<String, String>> getDiff(
            @RequestParam Long oldId,
            @RequestParam Long newId) {
        String diff = versionService.getDiff(oldId, newId);
        return ResponseEntity.ok(Map.of("diff", diff));
    }

    // ─── Branch Management ────────────────────────────────────────────────────

    /**
     * Create a new branch for a file by copying the latest snapshot from
     * the source branch.  Request body:
     * { "fileId": 1, "sourceBranch": "main", "newBranch": "feature/x" }
     */
    @PostMapping("/branches")
    public ResponseEntity<SnapshotDto> createBranch(
            @RequestBody Map<String, String> body,
            @RequestHeader("X-User") String userEmail) {
        Long fileId      = Long.parseLong(body.get("fileId"));
        String srcBranch = body.getOrDefault("sourceBranch", "main");
        String newBranch = body.get("newBranch");
        return new ResponseEntity<>(
                versionService.createBranch(fileId, srcBranch, newBranch, userEmail),
                HttpStatus.CREATED);
    }
}
