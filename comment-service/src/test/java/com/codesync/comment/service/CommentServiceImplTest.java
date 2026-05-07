package com.codesync.comment.service;

import com.codesync.comment.client.NotificationClient;
import com.codesync.comment.dto.CommentDto;
import com.codesync.comment.entity.Comment;
import com.codesync.comment.exception.ResourceNotFoundException;
import com.codesync.comment.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock private CommentRepository commentRepository;
    @Mock private NotificationClient notificationClient;

    @InjectMocks
    private CommentServiceImpl commentService;

    private Comment comment;
    private CommentDto dto;

    private static final String USER_EMAIL  = "author@example.com";
    private static final String OTHER_EMAIL = "other@example.com";

    @BeforeEach
    void setUp() {
        comment = Comment.builder()
                .commentId(1L)
                .projectId(100L)
                .fileId(200L)
                .authorEmail(USER_EMAIL)
                .content("Original content")
                .lineNumber(12)
                .columnNumber(8)
                .parentCommentId(null)
                .resolved(false)
                .snapshotId(7L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        dto = CommentDto.builder()
                .projectId(100L)
                .fileId(200L)
                .content("Original content")
                .lineNumber(12)
                .columnNumber(8)
                .parentCommentId(null)
                .snapshotId(7L)
                .build();
    }

    // ─── addComment ─────────────────────────────────────────────────────────────

    @Test
    void addComment_shouldPersistAndReturnDto() {
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        CommentDto result = commentService.addComment(dto, USER_EMAIL);

        assertNotNull(result);
        assertEquals(1L, result.getCommentId());
        assertEquals(USER_EMAIL, result.getAuthorEmail());
        assertEquals("Original content", result.getContent());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void addComment_shouldMapAllFields() {
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        CommentDto result = commentService.addComment(dto, USER_EMAIL);

        assertEquals(100L, result.getProjectId());
        assertEquals(200L, result.getFileId());
        assertEquals(12, result.getLineNumber());
        assertEquals(8, result.getColumnNumber());
        assertEquals(7L, result.getSnapshotId());
        assertFalse(result.getResolved());
    }

    @Test
    void addComment_shouldDispatchMentionNotificationForMentionedEmail() {
        dto = CommentDto.builder()
                .projectId(100L).fileId(200L).content("Hey @other@example.com check this").build();
        comment.setContent("Hey @other@example.com check this");
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        commentService.addComment(dto, USER_EMAIL);

        verify(notificationClient).sendMentionNotification(
                eq("other@example.com"), eq(USER_EMAIL), eq(1L), eq(200L));
    }

    @Test
    void addComment_shouldNotSendSelfMentionNotification() {
        dto = CommentDto.builder()
                .projectId(100L).fileId(200L).content("@author@example.com self mention").build();
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        commentService.addComment(dto, USER_EMAIL);

        verify(notificationClient, never()).sendMentionNotification(any(), any(), any(), any());
    }

    // ─── getCommentsByFile ──────────────────────────────────────────────────────

    @Test
    void getCommentsByFile_shouldReturnTopLevelComments() {
        when(commentRepository.findByFileIdAndParentCommentIdIsNullOrderByCreatedAtAsc(200L))
                .thenReturn(List.of(comment));

        List<CommentDto> results = commentService.getCommentsByFile(200L);

        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getCommentId());
    }

    @Test
    void getCommentsByFile_shouldReturnEmptyWhenNone() {
        when(commentRepository.findByFileIdAndParentCommentIdIsNullOrderByCreatedAtAsc(200L))
                .thenReturn(List.of());

        assertTrue(commentService.getCommentsByFile(200L).isEmpty());
    }

    // ─── getCommentsByProject ───────────────────────────────────────────────────

    @Test
    void getCommentsByProject_shouldReturnAllComments() {
        when(commentRepository.findByProjectId(100L)).thenReturn(List.of(comment));

        List<CommentDto> results = commentService.getCommentsByProject(100L);

        assertEquals(1, results.size());
        assertEquals(100L, results.get(0).getProjectId());
    }

    // ─── getCommentById ─────────────────────────────────────────────────────────

    @Test
    void getCommentById_shouldReturnDto() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        CommentDto result = commentService.getCommentById(1L);

        assertEquals(1L, result.getCommentId());
    }

    @Test
    void getCommentById_shouldThrowWhenNotFound() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> commentService.getCommentById(99L));
    }

    // ─── getReplies ─────────────────────────────────────────────────────────────

    @Test
    void getReplies_shouldReturnChildComments() {
        Comment reply = Comment.builder().commentId(2L).parentCommentId(1L)
                .authorEmail(USER_EMAIL).fileId(200L).build();
        when(commentRepository.findByParentCommentIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(reply));

        List<CommentDto> results = commentService.getReplies(1L);

        assertEquals(1, results.size());
        assertEquals(2L, results.get(0).getCommentId());
    }

    @Test
    void getReplies_shouldReturnEmptyWhenNoReplies() {
        when(commentRepository.findByParentCommentIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());

        assertTrue(commentService.getReplies(1L).isEmpty());
    }

    // ─── getCommentsByLine ──────────────────────────────────────────────────────

    @Test
    void getCommentsByLine_shouldReturnMatchingComments() {
        when(commentRepository.findByFileIdAndLineNumber(200L, 12)).thenReturn(List.of(comment));

        List<CommentDto> results = commentService.getCommentsByLine(200L, 12);

        assertEquals(1, results.size());
        assertEquals(12, results.get(0).getLineNumber());
    }

    // ─── getCommentCount ────────────────────────────────────────────────────────

    @Test
    void getCommentCount_shouldReturnCount() {
        when(commentRepository.countByFileId(200L)).thenReturn(5L);

        assertEquals(5L, commentService.getCommentCount(200L));
    }

    // ─── updateComment ──────────────────────────────────────────────────────────

    @Test
    void updateComment_shouldUpdateContent() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        CommentDto result = commentService.updateComment(1L, "Updated content", USER_EMAIL);

        assertEquals("Updated content", result.getContent());
        verify(commentRepository).save(comment);
    }

    @Test
    void updateComment_shouldRejectNonAuthor() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThrows(SecurityException.class,
                () -> commentService.updateComment(1L, "Updated", OTHER_EMAIL));
        verify(commentRepository, never()).save(any());
    }

    @Test
    void updateComment_shouldThrowWhenCommentNotFound() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> commentService.updateComment(99L, "Updated", USER_EMAIL));
    }

    @Test
    void updateComment_shouldDispatchMentionOnUpdate() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        commentService.updateComment(1L, "Hey @other@example.com", USER_EMAIL);

        verify(notificationClient).sendMentionNotification(
                eq("other@example.com"), eq(USER_EMAIL), eq(1L), eq(200L));
    }

    // ─── deleteComment ──────────────────────────────────────────────────────────

    @Test
    void deleteComment_shouldRemoveComment() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        commentService.deleteComment(1L, USER_EMAIL);

        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteComment_shouldRejectNonAuthor() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThrows(SecurityException.class,
                () -> commentService.deleteComment(1L, OTHER_EMAIL));
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void deleteComment_shouldThrowWhenNotFound() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> commentService.deleteComment(99L, USER_EMAIL));
    }

    // ─── resolveComment ─────────────────────────────────────────────────────────

    @Test
    void resolveComment_shouldSetResolvedTrue() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        CommentDto result = commentService.resolveComment(1L, USER_EMAIL);

        assertTrue(result.getResolved());
        verify(commentRepository).save(comment);
    }

    @Test
    void resolveComment_shouldThrowWhenNotFound() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> commentService.resolveComment(99L, USER_EMAIL));
    }

    // ─── unresolveComment ───────────────────────────────────────────────────────

    @Test
    void unresolveComment_shouldSetResolvedFalse() {
        comment.setResolved(true);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        CommentDto result = commentService.unresolveComment(1L, USER_EMAIL);

        assertFalse(result.getResolved());
        verify(commentRepository).save(comment);
    }

    @Test
    void unresolveComment_shouldThrowWhenNotFound() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> commentService.unresolveComment(99L, USER_EMAIL));
    }
}