package com.codesync.version.controller;

import com.codesync.version.dto.SnapshotDto;
import com.codesync.version.service.VersionService;
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

@WebMvcTest(VersionController.class)
@AutoConfigureMockMvc(addFilters = false)
class VersionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private VersionService versionService;

	private SnapshotDto snapshotDto;
	private static final String USER_EMAIL = "test@example.com";

	@BeforeEach
	void setUp() {
		snapshotDto = SnapshotDto.builder()
				.snapshotId(1L)
				.projectId(100L)
				.fileId(200L)
				.content("line1\nline2")
				.commitMessage("initial")
				.createdByEmail(USER_EMAIL)
				.branchName("main")
				.tag("v1")
				.createdAt(LocalDateTime.now())
				.build();
	}

	@Test
	void createSnapshot_shouldReturnCreatedSnapshot() throws Exception {
		when(versionService.createSnapshot(any(SnapshotDto.class), eq(USER_EMAIL))).thenReturn(snapshotDto);

		mockMvc.perform(post("/versions/snapshots")
					.header("X-User", USER_EMAIL)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(snapshotDto)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.snapshotId").value(1L))
				.andExpect(jsonPath("$.branchName").value("main"));

		verify(versionService).createSnapshot(any(SnapshotDto.class), eq(USER_EMAIL));
	}

	@Test
	void getSnapshot_shouldReturnSnapshot() throws Exception {
		when(versionService.getSnapshotById(1L)).thenReturn(snapshotDto);

		mockMvc.perform(get("/versions/snapshots/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.snapshotId").value(1L));

		verify(versionService).getSnapshotById(1L);
	}

	@Test
	void getFileHistory_shouldReturnList() throws Exception {
		when(versionService.getFileHistory(200L, "main")).thenReturn(List.of(snapshotDto));

		mockMvc.perform(get("/versions/files/200/history"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].fileId").value(200L));

		verify(versionService).getFileHistory(200L, "main");
	}

	@Test
	void getLatestSnapshot_shouldReturnSnapshot() throws Exception {
		when(versionService.getLatestSnapshot(200L, "main")).thenReturn(snapshotDto);

		mockMvc.perform(get("/versions/files/200/latest"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.snapshotId").value(1L));

		verify(versionService).getLatestSnapshot(200L, "main");
	}

	@Test
	void getDiff_shouldReturnDiffMap() throws Exception {
		when(versionService.getDiff(1L, 2L)).thenReturn("--- old\n+++ new");

		mockMvc.perform(get("/versions/diff").param("oldId", "1").param("newId", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.diff").value("--- old\n+++ new"));

		verify(versionService).getDiff(1L, 2L);
	}

	@Test
	void tagSnapshot_shouldReturnTaggedSnapshot() throws Exception {
		when(versionService.tagSnapshot(1L, "release-1")).thenReturn(snapshotDto);

		mockMvc.perform(post("/versions/snapshots/1/tag")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(Map.of("tag", "release-1"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.snapshotId").value(1L));

		verify(versionService).tagSnapshot(1L, "release-1");
	}
}