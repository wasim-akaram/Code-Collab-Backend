package com.codesync.execution.controller;

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

import com.codesync.execution.dto.ExecutionJobDto;
import com.codesync.execution.service.ExecutionService;

/** Unit tests for {@link ExecutionController}. */
@ExtendWith(MockitoExtension.class)
class ExecutionControllerTest {

    @Mock private ExecutionService executionService;
    @InjectMocks private ExecutionController controller;
    private static final String USER = "user@test.com";

    @Test @DisplayName("submitExecution returns 201")
    void submitExecution() {
        ExecutionJobDto dto = new ExecutionJobDto();
        when(executionService.submitExecution(any(), eq(USER))).thenReturn(dto);
        assertEquals(HttpStatus.CREATED, controller.submitExecution(dto, USER).getStatusCode());
    }

    @Test @DisplayName("getJobById returns job")
    void getJobById() {
        ExecutionJobDto dto = new ExecutionJobDto();
        when(executionService.getJobById("j1")).thenReturn(dto);
        assertEquals(HttpStatus.OK, controller.getJobById("j1").getStatusCode());
    }

    @Test @DisplayName("getExecutionsByUser returns list")
    void getExecutionsByUser() {
        when(executionService.getExecutionsByUser(USER)).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.getExecutionsByUser(USER).getStatusCode());
    }

    @Test @DisplayName("getExecutionsByProject returns list")
    void getExecutionsByProject() {
        when(executionService.getExecutionsByProject(1L)).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.getExecutionsByProject(1L).getStatusCode());
    }

    @Test @DisplayName("cancelExecution returns 200")
    void cancelExecution() {
        doNothing().when(executionService).cancelExecution("j1", USER);
        assertEquals(HttpStatus.OK, controller.cancelExecution("j1", USER).getStatusCode());
    }

    @Test @DisplayName("getExecutionResult returns result")
    void getExecutionResult() {
        ExecutionJobDto dto = new ExecutionJobDto();
        when(executionService.getExecutionResult("j1")).thenReturn(dto);
        assertEquals(HttpStatus.OK, controller.getExecutionResult("j1").getStatusCode());
    }

    @Test @DisplayName("getSupportedLanguages returns list")
    void getSupportedLanguages() {
        when(executionService.getSupportedLanguages()).thenReturn(List.of("java", "python"));
        assertEquals(2, controller.getSupportedLanguages().getBody().size());
    }

    @Test @DisplayName("getLanguageVersion returns version map")
    void getLanguageVersion() {
        when(executionService.getLanguageVersion("java")).thenReturn("17");
        var resp = controller.getLanguageVersion("java").getBody();
        assertEquals("java", resp.get("language"));
        assertEquals("17", resp.get("version"));
    }

    @Test @DisplayName("getExecutionStats returns stats")
    void getExecutionStats() {
        when(executionService.getExecutionStats(1L)).thenReturn(Map.of("total", 5));
        assertEquals(HttpStatus.OK, controller.getExecutionStats(1L).getStatusCode());
    }

    @Test @DisplayName("getAllExecutionsAdmin returns all jobs")
    void getAllExecutionsAdmin() {
        when(executionService.getAllExecutionsAdmin()).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.getAllExecutionsAdmin().getStatusCode());
    }

    @Test @DisplayName("getPlatformStats returns platform stats")
    void getPlatformStats() {
        when(executionService.getPlatformStats()).thenReturn(Map.of());
        assertEquals(HttpStatus.OK, controller.getPlatformStats().getStatusCode());
    }
}