package com.codesync.comment.controller;

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

import com.codesync.comment.dto.CommentDto;
import com.codesync.comment.service.CommentService;

/** Unit tests for {@link CommentController}. */
@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock private CommentService commentService;
    @InjectMocks private CommentController controller;
    private static final String USER = "user@test.com";

    @Test @DisplayName("addComment should return 201")
    void addComment() {
        CommentDto dto = new CommentDto();
        when(commentService.addComment(any(), eq(USER))).thenReturn(dto);
        assertEquals(HttpStatus.CREATED, controller.addComment(dto, USER).getStatusCode());
    }

    @Test @DisplayName("getCommentsByFile returns list")
    void getCommentsByFile() {
        when(commentService.getCommentsByFile(1L)).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.getCommentsByFile(1L).getStatusCode());
    }

    @Test @DisplayName("getCommentsByProject returns list")
    void getCommentsByProject() {
        when(commentService.getCommentsByProject(1L)).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.getCommentsByProject(1L).getStatusCode());
    }

    @Test @DisplayName("getCommentById returns comment")
    void getCommentById() {
        CommentDto dto = new CommentDto();
        when(commentService.getCommentById(1L)).thenReturn(dto);
        assertEquals(HttpStatus.OK, controller.getCommentById(1L).getStatusCode());
    }

    @Test @DisplayName("getReplies returns reply list")
    void getReplies() {
        when(commentService.getReplies(1L)).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.getReplies(1L).getStatusCode());
    }

    @Test @DisplayName("getCommentsByLine returns line-anchored comments")
    void getCommentsByLine() {
        when(commentService.getCommentsByLine(1L, 10)).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.getCommentsByLine(1L, 10).getStatusCode());
    }

    @Test @DisplayName("getCommentCount returns count")
    void getCommentCount() {
        when(commentService.getCommentCount(1L)).thenReturn(5L);
        assertEquals(5L, controller.getCommentCount(1L).getBody());
    }

    @Test @DisplayName("updateComment updates and returns")
    void updateComment() {
        CommentDto dto = new CommentDto();
        when(commentService.updateComment(1L, "new", USER)).thenReturn(dto);
        assertEquals(HttpStatus.OK, controller.updateComment(1L, Map.of("content", "new"), USER).getStatusCode());
    }

    @Test @DisplayName("deleteComment returns 204")
    void deleteComment() {
        doNothing().when(commentService).deleteComment(1L, USER);
        assertEquals(HttpStatus.NO_CONTENT, controller.deleteComment(1L, USER).getStatusCode());
    }

    @Test @DisplayName("resolveComment returns resolved comment")
    void resolveComment() {
        CommentDto dto = new CommentDto();
        when(commentService.resolveComment(1L, USER)).thenReturn(dto);
        assertEquals(HttpStatus.OK, controller.resolveComment(1L, USER).getStatusCode());
    }

    @Test @DisplayName("unresolveComment returns unresolved comment")
    void unresolveComment() {
        CommentDto dto = new CommentDto();
        when(commentService.unresolveComment(1L, USER)).thenReturn(dto);
        assertEquals(HttpStatus.OK, controller.unresolveComment(1L, USER).getStatusCode());
    }
}