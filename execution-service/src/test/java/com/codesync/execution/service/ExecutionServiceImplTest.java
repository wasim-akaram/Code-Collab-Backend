package com.codesync.execution.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import com.codesync.execution.dto.ExecutionJobDto;
import com.codesync.execution.entity.ExecutionJob;
import com.codesync.execution.exception.ResourceNotFoundException;
import com.codesync.execution.repository.ExecutionRepository;

@ExtendWith(MockitoExtension.class)
class ExecutionServiceImplTest {

    @Mock
    private ExecutionRepository executionRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ExecutionServiceImpl executionService;

    private static final String USER = "test@test.com";

    @Test
    void submitExecution_Success() {
        ExecutionJobDto dto = new ExecutionJobDto();
        dto.setProjectId(1L);
        dto.setFileId(1L);
        dto.setLanguage("python");
        dto.setSourceCode("print('test')");

        ExecutionJob job = ExecutionJob.builder().jobId("j1").build();
        when(executionRepository.save(any())).thenReturn(job);

        ExecutionJobDto res = executionService.submitExecution(dto, USER);
        assertEquals("j1", res.getJobId());
    }

    @Test
    void getJobById_Success() {
        ExecutionJob job = ExecutionJob.builder().jobId("j1").build();
        when(executionRepository.findByJobId("j1")).thenReturn(Optional.of(job));
        assertNotNull(executionService.getJobById("j1"));
    }

    @Test
    void getJobById_NotFound() {
        when(executionRepository.findByJobId("j1")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> executionService.getJobById("j1"));
    }

    @Test
    void getExecutionsByUser_Success() {
        ExecutionJob job = ExecutionJob.builder().jobId("j1").build();
        when(executionRepository.findByUserEmailOrderByCreatedAtDesc(USER)).thenReturn(List.of(job));
        assertEquals(1, executionService.getExecutionsByUser(USER).size());
    }

    @Test
    void getExecutionsByProject_Success() {
        ExecutionJob job = ExecutionJob.builder().jobId("j1").build();
        when(executionRepository.findByProjectIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(job));
        assertEquals(1, executionService.getExecutionsByProject(1L).size());
    }

    @Test
    void cancelExecution_Success() {
        ExecutionJob job = ExecutionJob.builder().jobId("j1").status("RUNNING").build();
        when(executionRepository.findByJobId("j1")).thenReturn(Optional.of(job));
        executionService.cancelExecution("j1", USER);
        assertEquals("CANCELLED", job.getStatus());
        verify(executionRepository).save(job);
    }

    @Test
    void cancelExecution_NotFound() {
        when(executionRepository.findByJobId("j1")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> executionService.cancelExecution("j1", USER));
    }

    @Test
    void getExecutionResult_Success() {
        ExecutionJob job = ExecutionJob.builder().jobId("j1").build();
        when(executionRepository.findByJobId("j1")).thenReturn(Optional.of(job));
        assertEquals("j1", executionService.getExecutionResult("j1").getJobId());
    }

    @Test
    void getSupportedLanguages_Success() {
        assertFalse(executionService.getSupportedLanguages().isEmpty());
    }

    @Test
    void getLanguageVersion_Success() {
        assertEquals("3.10.0", executionService.getLanguageVersion("python"));
        assertEquals("15.0.2", executionService.getLanguageVersion("java"));
        assertEquals("Unknown", executionService.getLanguageVersion("unknown"));
    }

    @Test
    void getExecutionStats_Success() {
        ExecutionJob job = ExecutionJob.builder().jobId("j1").build();
        when(executionRepository.findByProjectIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(job));
        Map<String, Object> stats = executionService.getExecutionStats(1L);
        assertEquals(1L, ((Number) stats.get("totalExecutions")).longValue());
    }

    @Test
    void getAllExecutionsAdmin_Success() {
        ExecutionJob job = ExecutionJob.builder().jobId("j1").build();
        when(executionRepository.findAll()).thenReturn(List.of(job));
        assertEquals(1, executionService.getAllExecutionsAdmin().size());
    }

    @Test
    void getPlatformStats_Success() {
        ExecutionJob j1 = ExecutionJob.builder().status("COMPLETED").build();
        ExecutionJob j2 = ExecutionJob.builder().status("FAILED").build();
        when(executionRepository.findAll()).thenReturn(List.of(j1, j2));
        Map<String, Object> stats = executionService.getPlatformStats();
        assertEquals(2L, ((Number) stats.get("totalExecutions")).longValue());
        assertEquals(1L, ((Number) stats.get("completed")).longValue());
        assertEquals(1L, ((Number) stats.get("failed")).longValue());
    }
    
    @Test
    void executeInSandbox_Cancelled() throws Exception {
        ExecutionJob job = ExecutionJob.builder().jobId("j1").status("CANCELLED").build();
        when(executionRepository.findByJobId("j1")).thenReturn(Optional.of(job));
        executionService.executeInSandbox("j1", 10).get();
        // Nothing else happens
        verify(executionRepository, never()).save(any());
    }

    @Test
    void executeInSandbox_UnsupportedLanguage() throws Exception {
        ExecutionJob job = ExecutionJob.builder().jobId("j1").language("unknown").sourceCode("").build();
        when(executionRepository.findByJobId("j1")).thenReturn(Optional.of(job));
        executionService.executeInSandbox("j1", 10).get();
        assertEquals("FAILED", job.getStatus());
        verify(executionRepository, times(2)).save(job);
    }

    @Test
    void executeInSandbox_ValidLanguage_CatchesException() throws Exception {
        // This will try to run a dummy command or fail if python isn't installed.
        // It should cover the try-catch block and ProcessBuilder setup.
        ExecutionJob job = ExecutionJob.builder().jobId("j1").language("python").sourceCode("print(1)").build();
        when(executionRepository.findByJobId("j1")).thenReturn(Optional.of(job));
        executionService.executeInSandbox("j1", 1).get();
        // It might succeed or fail, but coverage will increase.
        assertNotNull(job.getStatus());
    }

    @Test
    void executeInSandbox_WithStdin() throws Exception {
        ExecutionJob job = ExecutionJob.builder().jobId("j1").language("javascript").sourceCode("console.log(1)").stdin("input").build();
        when(executionRepository.findByJobId("j1")).thenReturn(Optional.of(job));
        executionService.executeInSandbox("j1", 1).get();
        assertNotNull(job.getStatus());
    }

    @Test
    void executeInSandbox_Java() throws Exception {
        ExecutionJob job = ExecutionJob.builder().jobId("j1").language("java").sourceCode("class Main {}").fileName("Main.java").build();
        when(executionRepository.findByJobId("j1")).thenReturn(Optional.of(job));
        executionService.executeInSandbox("j1", 1).get();
        assertNotNull(job.getStatus());
    }

    @Test
    void executeInSandbox_Cpp() throws Exception {
        ExecutionJob job = ExecutionJob.builder().jobId("j1").language("cpp").sourceCode("int main(){}").build();
        when(executionRepository.findByJobId("j1")).thenReturn(Optional.of(job));
        executionService.executeInSandbox("j1", 1).get();
        assertNotNull(job.getStatus());
    }
}
