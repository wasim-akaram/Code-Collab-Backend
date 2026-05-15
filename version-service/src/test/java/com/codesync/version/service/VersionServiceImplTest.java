package com.codesync.version.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codesync.version.dto.SnapshotDto;
import com.codesync.version.entity.Snapshot;
import com.codesync.version.exception.ResourceNotFoundException;
import com.codesync.version.repository.SnapshotRepository;

@ExtendWith(MockitoExtension.class)
class VersionServiceImplTest {

    @Mock
    private SnapshotRepository snapshotRepository;

    @InjectMocks
    private VersionServiceImpl versionService;

    private static final String USER = "test@test.com";

    @Test
    void createSnapshot_Success() {
        SnapshotDto dto = new SnapshotDto();
        dto.setProjectId(1L);
        dto.setFileId(1L);
        dto.setContent("test content");

        Snapshot parent = Snapshot.builder().snapshotId(5L).build();
        when(snapshotRepository.findFirstByFileIdAndBranchNameOrderByCreatedAtDesc(1L, "main"))
                .thenReturn(Optional.of(parent));

        when(snapshotRepository.save(any())).thenAnswer(i -> {
            Snapshot s = i.getArgument(0);
            s.setSnapshotId(10L);
            return s;
        });

        SnapshotDto res = versionService.createSnapshot(dto, USER);
        assertEquals(10L, res.getSnapshotId());
        assertEquals(5L, res.getParentSnapshotId());
        assertNotNull(res.getHash());
    }

    @Test
    void getFileHistory_Success() {
        Snapshot s = Snapshot.builder().snapshotId(1L).build();
        when(snapshotRepository.findByFileIdAndBranchNameOrderByCreatedAtDesc(1L, "main")).thenReturn(List.of(s));

        List<SnapshotDto> res = versionService.getFileHistory(1L, null);
        assertEquals(1, res.size());
    }

    @Test
    void getSnapshotsByFile_Success() {
        Snapshot s = Snapshot.builder().snapshotId(1L).build();
        when(snapshotRepository.findByFileIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(s));
        assertEquals(1, versionService.getSnapshotsByFile(1L).size());
    }

    @Test
    void getSnapshotsByProject_Success() {
        Snapshot s = Snapshot.builder().snapshotId(1L).build();
        when(snapshotRepository.findByProjectIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(s));
        assertEquals(1, versionService.getSnapshotsByProject(1L).size());
    }

    @Test
    void getSnapshotsByBranch_Success() {
        Snapshot s = Snapshot.builder().snapshotId(1L).build();
        when(snapshotRepository.findByProjectIdAndBranchNameOrderByCreatedAtDesc(1L, "main")).thenReturn(List.of(s));
        assertEquals(1, versionService.getSnapshotsByBranch(1L, "main").size());
    }

    @Test
    void getSnapshotById_Success() {
        Snapshot s = Snapshot.builder().snapshotId(1L).build();
        when(snapshotRepository.findById(1L)).thenReturn(Optional.of(s));
        assertNotNull(versionService.getSnapshotById(1L));
    }

    @Test
    void getLatestSnapshot_NotFound() {
        when(snapshotRepository.findFirstByFileIdAndBranchNameOrderByCreatedAtDesc(1L, "main")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> versionService.getLatestSnapshot(1L, null));
    }

    @Test
    void restoreSnapshot_Success() {
        Snapshot s = Snapshot.builder().snapshotId(1L).content("c").fileId(1L).branchName("main").build();
        when(snapshotRepository.findById(1L)).thenReturn(Optional.of(s));
        when(snapshotRepository.findFirstByFileIdAndBranchNameOrderByCreatedAtDesc(1L, "main")).thenReturn(Optional.of(s));
        when(snapshotRepository.save(any())).thenAnswer(i -> {
            Snapshot r = i.getArgument(0);
            r.setSnapshotId(2L);
            return r;
        });

        SnapshotDto res = versionService.restoreSnapshot(1L, USER);
        assertEquals(2L, res.getSnapshotId());
    }

    @Test
    void getDiff_Success() {
        Snapshot s1 = Snapshot.builder().snapshotId(1L).content("line1\nline2").build();
        Snapshot s2 = Snapshot.builder().snapshotId(2L).content("line1\nline3").build();
        when(snapshotRepository.findById(1L)).thenReturn(Optional.of(s1));
        when(snapshotRepository.findById(2L)).thenReturn(Optional.of(s2));

        String diff = versionService.getDiff(1L, 2L);
        assertTrue(diff.contains("line2"));
        assertTrue(diff.contains("line3"));
    }

    @Test
    void tagSnapshot_Success() {
        Snapshot s = Snapshot.builder().snapshotId(1L).build();
        when(snapshotRepository.findById(1L)).thenReturn(Optional.of(s));
        when(snapshotRepository.save(any())).thenReturn(s);

        versionService.tagSnapshot(1L, "v1");
        verify(snapshotRepository).save(s);
        assertEquals("v1", s.getTag());
    }

    @Test
    void createBranch_Success() {
        Snapshot src = Snapshot.builder().snapshotId(1L).content("c").fileId(1L).projectId(1L).build();
        when(snapshotRepository.findFirstByFileIdAndBranchNameOrderByCreatedAtDesc(1L, "main")).thenReturn(Optional.of(src));
        when(snapshotRepository.save(any())).thenAnswer(i -> {
            Snapshot s = i.getArgument(0);
            s.setSnapshotId(2L);
            return s;
        });

        SnapshotDto res = versionService.createBranch(1L, null, "dev", USER);
        assertEquals(2L, res.getSnapshotId());
        assertEquals("dev", res.getBranchName());
    }

    @Test
    void getBranches_Success() {
        when(snapshotRepository.findDistinctBranchNamesByProjectId(1L)).thenReturn(List.of("main", "dev"));
        assertEquals(2, versionService.getBranches(1L).size());
    }
}
