package com.codesync.file.controller;

import com.codesync.file.dto.CodeFileDto;
import com.codesync.file.service.FileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FileController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

    @Autowired
    private ObjectMapper objectMapper;

    private CodeFileDto mockFileDto;
    private static final String USER_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        mockFileDto = CodeFileDto.builder()
                .fileId(1L)
                .projectId(100L)
                .name("main.js")
                .path("src/main.js")
                .content("console.log('test')")
                .build();
    }

    @Test
    void createFile() throws Exception {
        when(fileService.createFile(any(CodeFileDto.class), eq(USER_EMAIL), any())).thenReturn(mockFileDto);

        mockMvc.perform(post("/files")
                .header("X-User", USER_EMAIL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mockFileDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileId").value(1L))
                .andExpect(jsonPath("$.path").value("src/main.js"));

        verify(fileService).createFile(any(CodeFileDto.class), eq(USER_EMAIL), any());
    }

    @Test
    void getFileById() throws Exception {
        when(fileService.getFileById(1L)).thenReturn(mockFileDto);

        mockMvc.perform(get("/files/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileId").value(1L));

        verify(fileService).getFileById(1L);
    }

    @Test
    void getFilesByProject() throws Exception {
        when(fileService.getFilesByProject(100L)).thenReturn(Arrays.asList(mockFileDto));

        mockMvc.perform(get("/files/project/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileId").value(1L));

        verify(fileService).getFilesByProject(100L);
    }

    @Test
    void updateFileContent() throws Exception {
        when(fileService.updateFileContent(eq(1L), eq("new content"), eq(USER_EMAIL), any())).thenReturn(mockFileDto);

        Map<String, String> body = new HashMap<>();
        body.put("content", "new content");

        mockMvc.perform(put("/files/1/content")
                .header("X-User", USER_EMAIL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        verify(fileService).updateFileContent(eq(1L), eq("new content"), eq(USER_EMAIL), any());
    }

    @Test
    void renameFile() throws Exception {
        when(fileService.renameFile(eq(1L), eq("new_name.js"), eq(USER_EMAIL), any())).thenReturn(mockFileDto);

        Map<String, String> body = new HashMap<>();
        body.put("newName", "new_name.js");

        mockMvc.perform(put("/files/1/rename")
                .header("X-User", USER_EMAIL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        verify(fileService).renameFile(eq(1L), eq("new_name.js"), eq(USER_EMAIL), any());
    }

    @Test
    void moveFile() throws Exception {
        when(fileService.moveFile(eq(1L), eq("/src/new_path"), eq(USER_EMAIL), any())).thenReturn(mockFileDto);

        Map<String, String> body = new HashMap<>();
        body.put("newPath", "/src/new_path");

        mockMvc.perform(put("/files/1/move")
                .header("X-User", USER_EMAIL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        verify(fileService).moveFile(eq(1L), eq("/src/new_path"), eq(USER_EMAIL), any());
    }

    @Test
    void deleteFile() throws Exception {
        mockMvc.perform(delete("/files/1"))
                .andExpect(status().isNoContent());

        verify(fileService).deleteFile(eq(1L), any(), any());
    }

    @Test
    void restoreFile() throws Exception {
        mockMvc.perform(post("/files/1/restore"))
                .andExpect(status().isOk());

        verify(fileService).restoreFile(eq(1L), any(), any());
    }
}
