package com.codesync.comment.controller;

import com.codesync.comment.dto.CommentDto;
import com.codesync.comment.service.CommentService;
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

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private CommentService commentService;

	private CommentDto commentDto;
	private static final String USER_EMAIL = "test@example.com";

	@BeforeEach
	void setUp() {
		commentDto = CommentDto.builder()
				.commentId(1L)
				.projectId(100L)
				.fileId(200L)
				.authorEmail(USER_EMAIL)
				.content("Looks good")
				.lineNumber(12)
				.columnNumber(8)
				.parentCommentId(null)
				.resolved(false)
				.snapshotId(7L)
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();
	}

	@Test
	void addComment_shouldReturnCreatedComment() throws Exception {
		when(commentService.addComment(any(CommentDto.class), eq(USER_EMAIL))).thenReturn(commentDto);

		mockMvc.perform(post("/comments")
					.header("X-User", USER_EMAIL)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(commentDto)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.commentId").value(1L))
				.andExpect(jsonPath("$.authorEmail").value(USER_EMAIL))
				.andExpect(jsonPath("$.content").value("Looks good"));

		verify(commentService).addComment(any(CommentDto.class), eq(USER_EMAIL));
	}

	@Test
	void getCommentsByFile_shouldReturnList() throws Exception {
		when(commentService.getCommentsByFile(200L)).thenReturn(List.of(commentDto));

		mockMvc.perform(get("/comments/file/200"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].commentId").value(1L))
				.andExpect(jsonPath("$[0].fileId").value(200L));

		verify(commentService).getCommentsByFile(200L);
	}

	@Test
	void getReplies_shouldReturnList() throws Exception {
		when(commentService.getReplies(1L)).thenReturn(List.of(commentDto));

		mockMvc.perform(get("/comments/1/replies"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].commentId").value(1L));

		verify(commentService).getReplies(1L);
	}

	@Test
	void updateComment_shouldReturnUpdatedComment() throws Exception {
		CommentDto updated = CommentDto.builder()
				.commentId(1L)
				.projectId(100L)
				.fileId(200L)
				.authorEmail(USER_EMAIL)
				.content("Updated comment")
				.build();
		when(commentService.updateComment(eq(1L), eq("Updated comment"), eq(USER_EMAIL))).thenReturn(updated);

		mockMvc.perform(put("/comments/1")
					.header("X-User", USER_EMAIL)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(Map.of("content", "Updated comment"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value("Updated comment"));

		verify(commentService).updateComment(eq(1L), eq("Updated comment"), eq(USER_EMAIL));
	}

	@Test
	void deleteComment_shouldReturnNoContent() throws Exception {
		mockMvc.perform(delete("/comments/1")
					.header("X-User", USER_EMAIL))
				.andExpect(status().isNoContent());

		verify(commentService).deleteComment(1L, USER_EMAIL);
	}

	@Test
	void resolveComment_shouldReturnResolvedComment() throws Exception {
		CommentDto resolved = CommentDto.builder()
				.commentId(1L)
				.resolved(true)
				.build();
		when(commentService.resolveComment(1L, USER_EMAIL)).thenReturn(resolved);

		mockMvc.perform(post("/comments/1/resolve")
					.header("X-User", USER_EMAIL))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resolved").value(true));

		verify(commentService).resolveComment(1L, USER_EMAIL);
	}

	@Test
	void unresolveComment_shouldReturnUnresolvedComment() throws Exception {
		CommentDto unresolved = CommentDto.builder()
				.commentId(1L)
				.resolved(false)
				.build();
		when(commentService.unresolveComment(1L, USER_EMAIL)).thenReturn(unresolved);

		mockMvc.perform(post("/comments/1/unresolve")
					.header("X-User", USER_EMAIL))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resolved").value(false));

		verify(commentService).unresolveComment(1L, USER_EMAIL);
	}
}