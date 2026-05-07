package com.codesync.execution.service;

import com.codesync.execution.dto.ExecutionJobDto;
import com.codesync.execution.entity.ExecutionJob;
import com.codesync.execution.exception.ResourceNotFoundException;
import com.codesync.execution.repository.ExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutionServiceImplTest {

	@Mock
	private ExecutionRepository executionRepository;

	@Mock
	private RestTemplate restTemplate;

	@InjectMocks
	private ExecutionServiceImpl executionService;

	private ExecutionJob job;
	private ExecutionJobDto dto;
	private static final String USER_EMAIL = "test@example.com";

	@BeforeEach
	void setUp() {
		job = ExecutionJob.builder()
				.id(1L)
				.jobId("job-1")
				.projectId(100L)
				.fileId(200L)
				.userEmail(USER_EMAIL)
				.language("java")
				.sourceCode("class Main {}")
				.stdin("input")
				.status("QUEUED")
				.stdout(null)
				.stderr(null)
				.exitCode(null)
				.executionTimeMs(null)
				.memoryUsedKb(null)
				.createdAt(LocalDateTime.now())
				.completedAt(null)
				.build();

		dto = ExecutionJobDto.builder()
				.projectId(100L)
				.fileId(200L)
				.language("java")
				.sourceCode("class Main {}")
				.stdin("input")
				.build();
	}

	@Test
	void submitExecution_shouldPersistQueuedJob() {
		when(executionRepository.save(any(ExecutionJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(executionRepository.findByJobId(any())).thenReturn(Optional.of(job));

		ExecutionJobDto result = executionService.submitExecution(dto, USER_EMAIL);

		assertNotNull(result.getJobId());
		assertEquals(USER_EMAIL, result.getUserEmail());
		verify(executionRepository, atLeastOnce()).save(any(ExecutionJob.class));
	}

	@Test
	void getJobById_shouldReturnMappedJob() {
		when(executionRepository.findByJobId("job-1")).thenReturn(Optional.of(job));

		ExecutionJobDto result = executionService.getJobById("job-1");

		assertEquals("job-1", result.getJobId());
		assertEquals("QUEUED", result.getStatus());
	}

	@Test
	void getJobById_missingJob_shouldThrow() {
		when(executionRepository.findByJobId("job-1")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> executionService.getJobById("job-1"));
	}

	@Test
	void getExecutionsByUser_shouldMapResults() {
		when(executionRepository.findByUserEmailOrderByCreatedAtDesc(USER_EMAIL)).thenReturn(List.of(job));

		List<ExecutionJobDto> results = executionService.getExecutionsByUser(USER_EMAIL);

		assertEquals(1, results.size());
		assertEquals("job-1", results.get(0).getJobId());
	}

	@Test
	void getExecutionsByProject_shouldMapResults() {
		when(executionRepository.findByProjectIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(job));

		List<ExecutionJobDto> results = executionService.getExecutionsByProject(100L);

		assertEquals(1, results.size());
		assertEquals(100L, results.get(0).getProjectId());
	}

	@Test
	void cancelExecution_shouldMarkQueuedJobCancelled() {
		when(executionRepository.findByJobId("job-1")).thenReturn(Optional.of(job));
		when(executionRepository.save(any(ExecutionJob.class))).thenReturn(job);

		executionService.cancelExecution("job-1", USER_EMAIL);

		assertEquals("CANCELLED", job.getStatus());
		assertNotNull(job.getCompletedAt());
		verify(executionRepository).save(job);
	}

	@Test
	void getExecutionResult_shouldDelegateToGetJobById() {
		when(executionRepository.findByJobId("job-1")).thenReturn(Optional.of(job));

		ExecutionJobDto result = executionService.getExecutionResult("job-1");

		assertEquals("job-1", result.getJobId());
	}

	@Test
	void getSupportedLanguages_shouldReturnConfiguredList() {
		List<String> langs = executionService.getSupportedLanguages();
		assertTrue(langs.contains("java"));
		assertTrue(langs.contains("python"));
		assertTrue(langs.contains("javascript"));
	}

	@Test
	void getLanguageVersion_shouldReturnKnownVersion() {
		assertEquals("15.0.2", executionService.getLanguageVersion("java"));
		assertEquals("3.10.0", executionService.getLanguageVersion("python"));
	}

	@Test
	void getExecutionStats_shouldReturnMap() {
		when(executionRepository.findByProjectIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(job));
		assertEquals(1L, executionService.getExecutionStats(100L).get("totalExecutions"));
	}
}