package com.codesync.execution.controller;

import com.codesync.execution.dto.ExecutionJobDto;
import com.codesync.execution.service.ExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExecutionController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExecutionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private ExecutionService executionService;

	private ExecutionJobDto jobDto;
	private static final String USER_EMAIL = "test@example.com";

	@BeforeEach
	void setUp() {
		jobDto = ExecutionJobDto.builder()
				.jobId("job-1")
				.projectId(100L)
				.fileId(200L)
				.userEmail(USER_EMAIL)
				.language("java")
				.sourceCode("class Main {}")
				.stdin("input")
				.status("QUEUED")
				.stdout("")
				.stderr("")
				.exitCode(null)
				.executionTimeMs(null)
				.memoryUsedKb(null)
				.createdAt(LocalDateTime.now())
				.completedAt(null)
				.build();
	}

	@Test
	void submitExecution_shouldReturnCreatedJob() throws Exception {
		when(executionService.submitExecution(any(ExecutionJobDto.class), eq(USER_EMAIL))).thenReturn(jobDto);

		mockMvc.perform(post("/executions")
					.header("X-User", USER_EMAIL)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(jobDto)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.jobId").value("job-1"))
				.andExpect(jsonPath("$.status").value("QUEUED"));

		verify(executionService).submitExecution(any(ExecutionJobDto.class), eq(USER_EMAIL));
	}

	@Test
	void getJobById_shouldReturnJob() throws Exception {
		when(executionService.getJobById("job-1")).thenReturn(jobDto);

		mockMvc.perform(get("/executions/job-1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.jobId").value("job-1"));

		verify(executionService).getJobById("job-1");
	}

	@Test
	void getExecutionsByUser_shouldReturnList() throws Exception {
		when(executionService.getExecutionsByUser(USER_EMAIL)).thenReturn(List.of(jobDto));

		mockMvc.perform(get("/executions/user")
					.header("X-User", USER_EMAIL))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].jobId").value("job-1"));

		verify(executionService).getExecutionsByUser(USER_EMAIL);
	}

	@Test
	void getExecutionsByProject_shouldReturnList() throws Exception {
		when(executionService.getExecutionsByProject(100L)).thenReturn(List.of(jobDto));

		mockMvc.perform(get("/executions/project/100"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].projectId").value(100L));

		verify(executionService).getExecutionsByProject(100L);
	}

	@Test
	void cancelExecution_shouldReturnOk() throws Exception {
		mockMvc.perform(post("/executions/job-1/cancel")
					.header("X-User", USER_EMAIL))
				.andExpect(status().isOk());

		verify(executionService).cancelExecution("job-1", USER_EMAIL);
	}

	@Test
	void getExecutionResult_shouldReturnJob() throws Exception {
		when(executionService.getExecutionResult("job-1")).thenReturn(jobDto);

		mockMvc.perform(get("/executions/job-1/result"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("QUEUED"));

		verify(executionService).getExecutionResult("job-1");
	}

	@Test
	void getSupportedLanguages_shouldReturnList() throws Exception {
		when(executionService.getSupportedLanguages()).thenReturn(List.of("java", "python"));

		mockMvc.perform(get("/executions/languages"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0]").value("java"));

		verify(executionService).getSupportedLanguages();
	}

	@Test
	void getLanguageVersion_shouldReturnMap() throws Exception {
		when(executionService.getLanguageVersion("java")).thenReturn("15.0.2");

		mockMvc.perform(get("/executions/languages/java/version"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.language").value("java"))
				.andExpect(jsonPath("$.version").value("15.0.2"));

		verify(executionService).getLanguageVersion("java");
	}

	@Test
	void getExecutionStats_shouldReturnStats() throws Exception {
		when(executionService.getExecutionStats(100L)).thenReturn(Map.of(
				"totalExecutions", 10,
				"projectId", 100
		));

		mockMvc.perform(get("/executions/stats/project/100"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalExecutions").value(10));

		verify(executionService).getExecutionStats(100L);
	}
}