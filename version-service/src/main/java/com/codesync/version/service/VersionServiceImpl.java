/*
 * Code reader note: Implements snapshot creation, lookup, history, latest lookup, restore snapshots, tags, branches, and simple diffs.
 */
package com.codesync.version.service;

import com.codesync.version.dto.SnapshotDto;
import com.codesync.version.entity.Snapshot;
import com.codesync.version.exception.ResourceNotFoundException;
import com.codesync.version.repository.SnapshotRepository;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the VersionService.
 * Key behaviours per documentation (Section 2.7):
 * - Every snapshot stores a SHA-256 content hash for integrity.
 * - Every snapshot stores the parentSnapshotId, forming a history DAG.
 * - Restore is NON-DESTRUCTIVE: it creates a new snapshot with old content.
 * - Branch creation copies the latest snapshot of a file from one branch to another.
 */
@Service
@RequiredArgsConstructor
public class VersionServiceImpl implements VersionService {

    private final SnapshotRepository snapshotRepository;

    // ─── Create ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public SnapshotDto createSnapshot(SnapshotDto dto, String userEmail) {
        // Missing branch means the normal default branch, matching Git-like
        // behavior without requiring the client to send "main" every time.
        String branch = dto.getBranchName() != null ? dto.getBranchName() : "main";

        // Auto-compute SHA-256 hash for integrity verification
        String hash = computeSha256(dto.getContent());

        // Auto-link to the most recent snapshot on this file+branch (history DAG)
        // parentSnapshotId lets the service reconstruct the chain of changes.
        Long parentId = snapshotRepository
                .findFirstByFileIdAndBranchNameOrderByCreatedAtDesc(dto.getFileId(), branch)
                .map(Snapshot::getSnapshotId)
                .orElse(null);

        Snapshot snapshot = Snapshot.builder()
                .projectId(dto.getProjectId())
                .fileId(dto.getFileId())
                .content(dto.getContent())
                .commitMessage(dto.getCommitMessage())
                .createdByEmail(userEmail)
                .branchName(branch)
                .tag(dto.getTag())
                .hash(hash)
                .parentSnapshotId(parentId)
                .build();

        return mapToDto(snapshotRepository.save(snapshot));
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    @Override
    public List<SnapshotDto> getFileHistory(Long fileId, String branchName) {
        String branch = branchName != null ? branchName : "main";
        return snapshotRepository.findByFileIdAndBranchNameOrderByCreatedAtDesc(fileId, branch)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public List<SnapshotDto> getSnapshotsByFile(Long fileId) {
        return snapshotRepository.findByFileIdOrderByCreatedAtDesc(fileId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public List<SnapshotDto> getSnapshotsByProject(Long projectId) {
        return snapshotRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public List<SnapshotDto> getSnapshotsByBranch(Long projectId, String branchName) {
        return snapshotRepository.findByProjectIdAndBranchNameOrderByCreatedAtDesc(projectId, branchName)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public SnapshotDto getSnapshotById(Long snapshotId) {
        return mapToDto(snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new ResourceNotFoundException("Snapshot not found: " + snapshotId)));
    }

    @Override
    public SnapshotDto getLatestSnapshot(Long fileId, String branchName) {
        String branch = branchName != null ? branchName : "main";
        return mapToDto(snapshotRepository.findFirstByFileIdAndBranchNameOrderByCreatedAtDesc(fileId, branch)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No snapshots found for file " + fileId + " on branch '" + branch + "'")));
    }

    // ─── Restore (non-destructive) ────────────────────────────────────────────

    @Override
    @Transactional
    public SnapshotDto restoreSnapshot(Long snapshotId, String userEmail) {
        Snapshot source = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new ResourceNotFoundException("Snapshot not found: " + snapshotId));

        // Non-destructive: create a brand-new snapshot with the old content
        String branch = source.getBranchName();
        String hash = computeSha256(source.getContent());

        Long parentId = snapshotRepository
                .findFirstByFileIdAndBranchNameOrderByCreatedAtDesc(source.getFileId(), branch)
                .map(Snapshot::getSnapshotId)
                .orElse(null);

        Snapshot restored = Snapshot.builder()
                .projectId(source.getProjectId())
                .fileId(source.getFileId())
                .content(source.getContent())
                .commitMessage("Restored from snapshot #" + snapshotId)
                .createdByEmail(userEmail)
                .branchName(branch)
                .hash(hash)
                .parentSnapshotId(parentId)
                .build();

        return mapToDto(snapshotRepository.save(restored));
    }

    // ─── Diff ─────────────────────────────────────────────────────────────────

    @Override
    public String getDiff(Long oldSnapshotId, Long newSnapshotId) {
        Snapshot oldSnapshot = snapshotRepository.findById(oldSnapshotId)
                .orElseThrow(() -> new ResourceNotFoundException("Old snapshot not found: " + oldSnapshotId));
        Snapshot newSnapshot = snapshotRepository.findById(newSnapshotId)
                .orElseThrow(() -> new ResourceNotFoundException("New snapshot not found: " + newSnapshotId));

        // java-diff-utils works on lists of lines, so file content is split before
        // generating a unified diff.
        List<String> original = Arrays.asList(oldSnapshot.getContent().split("\n"));
        List<String> revised  = Arrays.asList(newSnapshot.getContent().split("\n"));

        Patch<String> patch = DiffUtils.diff(original, revised);
        // Context size 3 means the diff includes three unchanged lines around
        // each changed block, similar to git diff output.
        List<String> diffOutput = UnifiedDiffUtils.generateUnifiedDiff(
                "snapshot_" + oldSnapshotId,
                "snapshot_" + newSnapshotId,
                original, patch, 3);

        return String.join("\n", diffOutput);
    }

    // ─── Tag ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public SnapshotDto tagSnapshot(Long snapshotId, String tag) {
        Snapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new ResourceNotFoundException("Snapshot not found: " + snapshotId));
        snapshot.setTag(tag);
        return mapToDto(snapshotRepository.save(snapshot));
    }

    // ─── Branch ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public SnapshotDto createBranch(Long fileId, String sourceBranch, String newBranch, String userEmail) {
        // Branch creation copies the latest content from sourceBranch and writes
        // it as the first snapshot on newBranch.
        String src = sourceBranch != null ? sourceBranch : "main";

        // Get the latest snapshot on the source branch to seed the new branch
        Snapshot source = snapshotRepository
                .findFirstByFileIdAndBranchNameOrderByCreatedAtDesc(fileId, src)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No snapshot found on branch '" + src + "' for file " + fileId));

        String hash = computeSha256(source.getContent());

        Snapshot branchHead = Snapshot.builder()
                .projectId(source.getProjectId())
                .fileId(fileId)
                .content(source.getContent())
                .commitMessage("Branched from '" + src + "' (snapshot #" + source.getSnapshotId() + ")")
                .createdByEmail(userEmail)
                .branchName(newBranch)
                .hash(hash)
                .parentSnapshotId(source.getSnapshotId())
                .build();

        return mapToDto(snapshotRepository.save(branchHead));
    }

    @Override
    public List<String> getBranches(Long projectId) {
        return snapshotRepository.findDistinctBranchNamesByProjectId(projectId);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private SnapshotDto mapToDto(Snapshot s) {
        // Convert persistence entities into DTOs so API callers do not depend on
        // JPA internals.
        return SnapshotDto.builder()
                .snapshotId(s.getSnapshotId())
                .projectId(s.getProjectId())
                .fileId(s.getFileId())
                .authorId(s.getAuthorId())
                .content(s.getContent())
                .commitMessage(s.getCommitMessage())
                .createdByEmail(s.getCreatedByEmail())
                .hash(s.getHash())
                .parentSnapshotId(s.getParentSnapshotId())
                .branchName(s.getBranchName())
                .tag(s.getTag())
                .createdAt(s.getCreatedAt())
                .build();
    }

    /**
     * Computes the SHA-256 hex digest of the given string.
     * Used to populate the {@code hash} field on every snapshot for content integrity.
     */
    private String computeSha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            // Store hashes as lowercase hexadecimal strings, which are compact and
            // easy to compare in APIs or logs.
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by the JVM spec — this should never happen
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
