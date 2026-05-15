package com.codesync.version.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.codesync.version.dto.SnapshotDto;
import com.codesync.version.service.VersionService;

/** Unit tests for {@link VersionController}. */
@ExtendWith(MockitoExtension.class)
class VersionControllerTest {

    @Mock private VersionService versionService;
    @InjectMocks private VersionController controller;
    private static final String USER = "user@test.com";

    @Test @DisplayName("createSnapshot returns 201")
    void createSnapshot() {
        SnapshotDto dto = new SnapshotDto();
        when(versionService.createSnapshot(any(), eq(USER))).thenReturn(dto);
        assertEquals(HttpStatus.CREATED, controller.createSnapshot(dto, USER).getStatusCode());
    }

    @Test @DisplayName("getSnapshot returns snapshot")
    void getSnapshot() {
        SnapshotDto dto = new SnapshotDto();
        when(versionService.getSnapshotById(1L)).thenReturn(dto);
        assertEquals(HttpStatus.OK, controller.getSnapshot(1L).getStatusCode());
    }

    @Test @DisplayName("tagSnapshot adds tag")
    void tagSnapshot() {
        SnapshotDto dto = new SnapshotDto();
        when(versionService.tagSnapshot(1L, "v1.0")).thenReturn(dto);
        assertEquals(HttpStatus.OK, controller.tagSnapshot(1L, Map.of("tag", "v1.0")).getStatusCode());
    }

    @Test @DisplayName("restoreSnapshot returns 201")
    void restoreSnapshot() {
        SnapshotDto dto = new SnapshotDto();
        when(versionService.restoreSnapshot(1L, USER)).thenReturn(dto);
        assertEquals(HttpStatus.CREATED, controller.restoreSnapshot(1L, USER).getStatusCode());
    }

    @Test @DisplayName("getFileHistory returns history list")
    void getFileHistory() {
        when(versionService.getFileHistory(1L, "main")).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.getFileHistory(1L, "main").getStatusCode());
    }

    @Test @DisplayName("getSnapshotsByFile returns all branches")
    void getSnapshotsByFile() {
        when(versionService.getSnapshotsByFile(1L)).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.getSnapshotsByFile(1L).getStatusCode());
    }

    @Test @DisplayName("getLatestSnapshot returns latest")
    void getLatestSnapshot() {
        SnapshotDto dto = new SnapshotDto();
        when(versionService.getLatestSnapshot(1L, "main")).thenReturn(dto);
        assertEquals(HttpStatus.OK, controller.getLatestSnapshot(1L, "main").getStatusCode());
    }

    @Test @DisplayName("getProjectSnapshots returns list")
    void getProjectSnapshots() {
        when(versionService.getSnapshotsByProject(1L)).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.getProjectSnapshots(1L).getStatusCode());
    }

    @Test @DisplayName("getBranches returns branch names")
    void getBranches() {
        when(versionService.getBranches(1L)).thenReturn(List.of("main", "dev"));
        assertEquals(2, controller.getBranches(1L).getBody().size());
    }

    @Test @DisplayName("getDiff returns diff map")
    void getDiff() {
        when(versionService.getDiff(1L, 2L)).thenReturn("--- old\n+++ new");
        var resp = controller.getDiff(1L, 2L).getBody();
        assertTrue(resp.get("diff").contains("---"));
    }

    @Test @DisplayName("createBranch returns 201")
    void createBranch() {
        SnapshotDto dto = new SnapshotDto();
        when(versionService.createBranch(1L, "main", "feature/x", USER)).thenReturn(dto);
        Map<String, String> body = new HashMap<>();
        body.put("fileId", "1");
        body.put("sourceBranch", "main");
        body.put("newBranch", "feature/x");
        assertEquals(HttpStatus.CREATED, controller.createBranch(body, USER).getStatusCode());
    }
}