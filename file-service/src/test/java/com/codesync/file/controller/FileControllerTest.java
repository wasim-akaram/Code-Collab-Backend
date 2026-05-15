package com.codesync.file.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import com.codesync.file.dto.CodeFileDto;
import com.codesync.file.service.FileService;

/**
 * Unit tests for {@link FileController}.
 * Covers all REST endpoints for file CRUD operations.
 */
@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock private FileService fileService;
    @InjectMocks private FileController controller;

    private static final String USER = "user@test.com";
    private static final String AUTH = "Bearer token";

    @Test @DisplayName("createFile should return 201")
    void createFile() {
        CodeFileDto dto = new CodeFileDto();
        dto.setProjectId(1L);
        dto.setName("Main.java");
        when(fileService.createFile(any(), eq(USER), eq(AUTH))).thenReturn(dto);

        ResponseEntity<CodeFileDto> resp = controller.createFile(dto, USER, AUTH);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
    }

    @Test @DisplayName("createFolder should return 201")
    void createFolder() {
        CodeFileDto dto = new CodeFileDto();
        when(fileService.createFolder(1L, "src", "main", USER, AUTH)).thenReturn(dto);

        Map<String, Object> body = Map.of("projectId", 1L, "path", "src", "name", "main");
        assertEquals(HttpStatus.CREATED, controller.createFolder(body, USER, AUTH).getStatusCode());
    }

    @Test @DisplayName("uploadFile should create file from multipart")
    void uploadFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        CodeFileDto dto = new CodeFileDto();
        when(fileService.createFile(any(), eq(USER), eq(AUTH))).thenReturn(dto);

        ResponseEntity<CodeFileDto> resp = controller.uploadFile(1L, "src/", file, USER, AUTH);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
    }

    @Test @DisplayName("uploadFile with empty path should use filename directly")
    void uploadFile_emptyPath() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());
        CodeFileDto dto = new CodeFileDto();
        when(fileService.createFile(any(), eq(USER), eq(AUTH))).thenReturn(dto);

        controller.uploadFile(1L, "", file, USER, AUTH);
        verify(fileService).createFile(argThat(d -> d.getPath().equals("test.txt")), eq(USER), eq(AUTH));
    }

    @Test @DisplayName("getFileById should return file")
    void getFileById() {
        CodeFileDto dto = new CodeFileDto();
        dto.setFileId(1L);
        when(fileService.getFileById(1L)).thenReturn(dto);

        assertEquals(1L, controller.getFileById(1L).getBody().getFileId());
    }

    @Test @DisplayName("getFileContent should return content string")
    void getFileContent() {
        when(fileService.getFileContent(1L)).thenReturn("code");
        assertEquals("code", controller.getFileContent(1L).getBody());
    }

    @Test @DisplayName("getFilesByProject should return file list")
    void getFilesByProject() {
        when(fileService.getFilesByProject(1L)).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.getFilesByProject(1L).getStatusCode());
    }

    @Test @DisplayName("getFileTree should return sorted tree")
    void getFileTree() {
        when(fileService.getFileTree(1L)).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.getFileTree(1L).getStatusCode());
    }

    @Test @DisplayName("searchInProject should return results")
    void searchInProject() {
        when(fileService.searchInProject(1L, "main")).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.searchInProject(1L, "main").getStatusCode());
    }

    @Test @DisplayName("updateFileContent should update and return")
    void updateFileContent() {
        CodeFileDto dto = new CodeFileDto();
        when(fileService.updateFileContent(1L, "new code", USER, AUTH)).thenReturn(dto);

        assertEquals(HttpStatus.OK, controller.updateFileContent(1L, Map.of("content", "new code"), USER, AUTH).getStatusCode());
    }

    @Test @DisplayName("renameFile should rename and return")
    void renameFile() {
        CodeFileDto dto = new CodeFileDto();
        when(fileService.renameFile(1L, "New.java", USER, AUTH)).thenReturn(dto);

        assertEquals(HttpStatus.OK, controller.renameFile(1L, Map.of("newName", "New.java"), USER, AUTH).getStatusCode());
    }

    @Test @DisplayName("moveFile should move and return")
    void moveFile() {
        CodeFileDto dto = new CodeFileDto();
        when(fileService.moveFile(1L, "src/main", USER, AUTH)).thenReturn(dto);

        assertEquals(HttpStatus.OK, controller.moveFile(1L, Map.of("newPath", "src/main"), USER, AUTH).getStatusCode());
    }

    @Test @DisplayName("deleteFile should return 204")
    void deleteFile() {
        doNothing().when(fileService).deleteFile(1L, USER, AUTH);
        assertEquals(HttpStatus.NO_CONTENT, controller.deleteFile(1L, USER, AUTH).getStatusCode());
    }

    @Test @DisplayName("restoreFile should return 200")
    void restoreFile() {
        doNothing().when(fileService).restoreFile(1L, USER, AUTH);
        assertEquals(HttpStatus.OK, controller.restoreFile(1L, USER, AUTH).getStatusCode());
    }
}
