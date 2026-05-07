package com.codesync.version.service;

import com.codesync.version.dto.SnapshotDto;
import com.codesync.version.entity.Snapshot;
import com.codesync.version.exception.ResourceNotFoundException;
import com.codesync.version.repository.SnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VersionServiceImplTest {

    @Mock
    private SnapshotRepository snapshotRepository;

    @InjectMocks
    private VersionServiceImpl versionService;

    private Snapshot snapshot;
    private SnapshotDto dto;
    private static final String USER_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        snapshot = Snapshot.builder()
                .snapshotId(1L)
                .projectId(100L)
                .fileId(200L)
                .content("line1\nline2")
                .commitMessage("initial commit")
                .createdByEmail(USER_EMAIL)
                .branchName("main")
                .tag("v1")
                .createdAt(LocalDateTime.now())
                .build();

        dto = SnapshotDto.builder()
                .projectId(100L)
                .fileId(200L)
                .content("line1\nline2")
                .commitMessage("initial commit")
                .branchName("main")
                .tag("v1")
                .build();
    }

    // ─── createSnapshot ─────────────────────────────────────────────────────────

    @Test
    void createSnapshot_shouldPersistAndReturnDto() {
        // No prior snapshot (first commit on branch)
        when(snapshotRepository.findFirstByFileIdAndBranchNameOrderByCreatedAtDesc(200L, "main"))
                .thenReturn(Optional.empty());
        when(snapshotRepository.save(any(Snapshot.class))).thenReturn(snapshot);

        SnapshotDto result = versionService.createSnapshot(dto, USER_EMAIL);

        assertNotNull(result);
        assertEquals(1L, result.getSnapshotId());
        assertEquals(USER_EMAIL, result.getCreatedByEmail());
        verify(snapshotRepository).save(any(Snapshot.class));
    }

    @Test
    void createSnapshot_shouldAutoComputeSha256Hash() {
        when(snapshotRepository.findFirstByFileIdAndBranchNameOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(snapshotRepository.save(any(Snapshot.class))).thenAnswer(inv -> {
            Snapshot s = inv.getArgument(0);
            assertNotNull(s.getHash());
            assertEquals(64, s.getHash().length()); // SHA-256 = 32 bytes = 64 hex chars
            return snapshot;
        });

        versionService.createSnapshot(dto, USER_EMAIL);
    }

    @Test
    void createSnapshot_shouldLinkParentSnapshotId() {
        Snapshot parent = Snapshot.builder().snapshotId(10L).build();
        when(snapshotRepository.findFirstByFileIdAndBranchNameOrderByCreatedAtDesc(200L, "main"))
                .thenReturn(Optional.of(parent));
        when(snapshotRepository.save(any(Snapshot.class))).thenAnswer(inv -> {
            Snapshot s = inv.getArgument(0);
            assertEquals(10L, s.getParentSnapshotId());
            return snapshot;
        });

        versionService.createSnapshot(dto, USER_EMAIL);
    }

    @Test
    void createSnapshot_shouldDefaultBranchToMainWhenNull() {
        dto = SnapshotDto.builder().projectId(100L).fileId(200L).content("x").build(); // no branchName
        when(snapshotRepository.findFirstByFileIdAndBranchNameOrderByCreatedAtDesc(200L, "main"))
                .thenReturn(Optional.empty());
        when(snapshotRepository.save(any(Snapshot.class))).thenAnswer(inv -> {
            Snapshot s = inv.getArgument(0);
            assertEquals("main", s.getBranchName());
            return snapshot;
        });

        versionService.createSnapshot(dto, USER_EMAIL);
    }

    // ─── getFileHistory ──────────────────────────────────────────────────────────

    @Test
    void getFileHistory_shouldMapResults() {
        when(snapshotRepository.findByFileIdAndBranchNameOrderByCreatedAtDesc(200L, "main"))
                .thenReturn(List.of(snapshot));

        List<SnapshotDto> results = versionService.getFileHistory(200L, "main");

        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getSnapshotId());
    }

    @Test
    void getFileHistory_shouldDefaultToMainBranch() {
        when(snapshotRepository.findByFileIdAndBranchNameOrderByCreatedAtDesc(200L, "main"))
                .thenReturn(List.of(snapshot));

        List<SnapshotDto> results = versionService.getFileHistory(200L, null);
        assertEquals(1, results.size());
    }

    // ─── getSnapshotsByFile / ByProject / ByBranch ──────────────────────────────

    @Test
    void getSnapshotsByFile_shouldReturnAllSnapshotsForFile() {
        when(snapshotRepository.findByFileIdOrderByCreatedAtDesc(200L)).thenReturn(List.of(snapshot));

        List<SnapshotDto> results = versionService.getSnapshotsByFile(200L);

        assertEquals(1, results.size());
        assertEquals(200L, results.get(0).getFileId());
    }

    @Test
    void getSnapshotsByProject_shouldReturnAllSnapshotsForProject() {
        when(snapshotRepository.findByProjectIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(snapshot));

        List<SnapshotDto> results = versionService.getSnapshotsByProject(100L);

        assertEquals(1, results.size());
        assertEquals(100L, results.get(0).getProjectId());
    }

    @Test
    void getSnapshotsByBranch_shouldFilterByBranch() {
        when(snapshotRepository.findByProjectIdAndBranchNameOrderByCreatedAtDesc(100L, "feature"))
                .thenReturn(List.of(snapshot));

        List<SnapshotDto> results = versionService.getSnapshotsByBranch(100L, "feature");

        assertEquals(1, results.size());
    }

    // ─── getSnapshotById ────────────────────────────────────────────────────────

    @Test
    void getSnapshotById_shouldReturnDto() {
        when(snapshotRepository.findById(1L)).thenReturn(Optional.of(snapshot));

        SnapshotDto result = versionService.getSnapshotById(1L);

        assertEquals(1L, result.getSnapshotId());
    }

    @Test
    void getSnapshotById_missingSnapshot_shouldThrow() {
        when(snapshotRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> versionService.getSnapshotById(1L));
    }

    // ─── getLatestSnapshot ──────────────────────────────────────────────────────

    @Test
    void getLatestSnapshot_shouldReturnMostRecent() {
        when(snapshotRepository.findFirstByFileIdAndBranchNameOrderByCreatedAtDesc(200L, "main"))
                .thenReturn(Optional.of(snapshot));

        SnapshotDto result = versionService.getLatestSnapshot(200L, "main");
        assertEquals(1L, result.getSnapshotId());
    }

    @Test
    void getLatestSnapshot_missingSnapshot_shouldThrow() {
        when(snapshotRepository.findFirstByFileIdAndBranchNameOrderByCreatedAtDesc(200L, "main"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> versionService.getLatestSnapshot(200L, "main"));
    }

    // ─── restoreSnapshot (non-destructive) ──────────────────────────────────────

    @Test
    void restoreSnapshot_shouldCreateNewSnapshotWithOldContent() {
        Snapshot restored = Snapshot.builder()
                .snapshotId(99L)
                .fileId(200L)
                .projectId(100L)
                .content("line1\nline2")
                .commitMessage("Restored from snapshot #1")
                .createdByEmail(USER_EMAIL)
                .branchName("main")
                .build();

        when(snapshotRepository.findById(1L)).thenReturn(Optional.of(snapshot));
        when(snapshotRepository.findFirstByFileIdAndBranchNameOrderByCreatedAtDesc(200L, "main"))
                .thenReturn(Optional.of(snapshot)); // current head
        when(snapshotRepository.save(any(Snapshot.class))).thenReturn(restored);

        SnapshotDto result = versionService.restoreSnapshot(1L, USER_EMAIL);

        assertNotNull(result);
        assertEquals(99L, result.getSnapshotId());
        assertEquals("line1\nline2", result.getContent());
        // Verify it does NOT modify the original snapshot — only saves a new one
        verify(snapshotRepository, times(1)).save(argThat(s ->
                "Restored from snapshot #1".equals(s.getCommitMessage()) &&
                USER_EMAIL.equals(s.getCreatedByEmail())
        ));
    }

    @Test
    void restoreSnapshot_shouldThrowWhenSnapshotNotFound() {
        when(snapshotRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> versionService.restoreSnapshot(99L, USER_EMAIL));
    }

    @Test
    void restoreSnapshot_shouldSetParentSnapshotId() {
        Snapshot currentHead = Snapshot.builder().snapshotId(5L).build();
        when(snapshotRepository.findById(1L)).thenReturn(Optional.of(snapshot));
        when(snapshotRepository.findFirstByFileIdAndBranchNameOrderByCreatedAtDesc(200L, "main"))
                .thenReturn(Optional.of(currentHead));
        when(snapshotRepository.save(any(Snapshot.class))).thenAnswer(inv -> {
            Snapshot s = inv.getArgument(0);
            assertEquals(5L, s.getParentSnapshotId());
            return s;
        });

        versionService.restoreSnapshot(1L, USER_EMAIL);
    }

    // ─── getDiff ────────────────────────────────────────────────────────────────

    @Test
    void getDiff_shouldReturnUnifiedDiff() {
        Snapshot old = Snapshot.builder().snapshotId(1L).fileId(200L).content("line1\nold").build();
        Snapshot neo = Snapshot.builder().snapshotId(2L).fileId(200L).content("line1\nnew").build();

        when(snapshotRepository.findById(1L)).thenReturn(Optional.of(old));
        when(snapshotRepository.findById(2L)).thenReturn(Optional.of(neo));

        String diff = versionService.getDiff(1L, 2L);

        // Real impl generates "--- snapshot_1" and "+++ snapshot_2" (fix for old assertion bug)
        assertTrue(diff.contains("--- snapshot_1"), "Diff should contain old snapshot header");
        assertTrue(diff.contains("+++ snapshot_2"), "Diff should contain new snapshot header");
        // Content diff assertions
        assertTrue(diff.contains("-old") || diff.contains("+new"),
                "Diff should show changed lines");
    }

    @Test
    void getDiff_shouldThrowWhenOldSnapshotMissing() {
        when(snapshotRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> versionService.getDiff(1L, 2L));
    }

    @Test
    void getDiff_shouldThrowWhenNewSnapshotMissing() {
        Snapshot old = Snapshot.builder().snapshotId(1L).content("x").build();
        when(snapshotRepository.findById(1L)).thenReturn(Optional.of(old));
        when(snapshotRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> versionService.getDiff(1L, 2L));
    }

    // ─── tagSnapshot ────────────────────────────────────────────────────────────

    @Test
    void tagSnapshot_shouldPersistTag() {
        when(snapshotRepository.findById(1L)).thenReturn(Optional.of(snapshot));
        when(snapshotRepository.save(any(Snapshot.class))).thenReturn(snapshot);

        SnapshotDto result = versionService.tagSnapshot(1L, "release-1.0");

        assertEquals("release-1.0", result.getTag());
        verify(snapshotRepository).save(snapshot);
    }

    @Test
    void tagSnapshot_shouldThrowWhenSnapshotMissing() {
        when(snapshotRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> versionService.tagSnapshot(99L, "v2"));
    }

    // ─── createBranch ───────────────────────────────────────────────────────────

    @Test
    void createBranch_shouldSeedNewBranchFromSourceHead() {
        Snapshot newHead = Snapshot.builder()
                .snapshotId(50L).fileId(200L).projectId(100L)
                .content("line1\nline2").branchName("feature")
                .createdByEmail(USER_EMAIL).build();

        when(snapshotRepository.findFirstByFileIdAndBranchNameOrderByCreatedAtDesc(200L, "main"))
                .thenReturn(Optional.of(snapshot));
        when(snapshotRepository.save(any(Snapshot.class))).thenReturn(newHead);

        SnapshotDto result = versionService.createBranch(200L, "main", "feature", USER_EMAIL);

        assertNotNull(result);
        assertEquals(50L, result.getSnapshotId());
        verify(snapshotRepository).save(argThat(s ->
                "feature".equals(s.getBranchName()) &&
                s.getParentSnapshotId().equals(snapshot.getSnapshotId())
        ));
    }

    @Test
    void createBranch_shouldThrowWhenSourceBranchHasNoSnapshots() {
        when(snapshotRepository.findFirstByFileIdAndBranchNameOrderByCreatedAtDesc(200L, "nonexistent"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> versionService.createBranch(200L, "nonexistent", "feature", USER_EMAIL));
    }

    @Test
    void createBranch_shouldDefaultSourceBranchToMain() {
        when(snapshotRepository.findFirstByFileIdAndBranchNameOrderByCreatedAtDesc(200L, "main"))
                .thenReturn(Optional.of(snapshot));
        when(snapshotRepository.save(any(Snapshot.class))).thenReturn(snapshot);

        // source branch null → should fall back to "main"
        versionService.createBranch(200L, null, "feature", USER_EMAIL);

        verify(snapshotRepository).findFirstByFileIdAndBranchNameOrderByCreatedAtDesc(200L, "main");
    }

    // ─── getBranches ────────────────────────────────────────────────────────────

    @Test
    void getBranches_shouldReturnDistinctBranchNames() {
        when(snapshotRepository.findDistinctBranchNamesByProjectId(100L))
                .thenReturn(List.of("main", "feature", "hotfix"));

        List<String> branches = versionService.getBranches(100L);

        assertEquals(3, branches.size());
        assertTrue(branches.contains("main"));
        assertTrue(branches.contains("feature"));
    }

    @Test
    void getBranches_shouldReturnEmptyListWhenNoSnapshots() {
        when(snapshotRepository.findDistinctBranchNamesByProjectId(100L)).thenReturn(List.of());

        List<String> branches = versionService.getBranches(100L);
        assertTrue(branches.isEmpty());
    }
}