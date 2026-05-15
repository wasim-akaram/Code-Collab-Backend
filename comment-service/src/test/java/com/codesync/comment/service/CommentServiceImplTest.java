package com.codesync.comment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codesync.comment.client.NotificationClient;
import com.codesync.comment.dto.CommentDto;
import com.codesync.comment.entity.Comment;
import com.codesync.comment.repository.CommentRepository;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private CommentServiceImpl commentService;

    private static final String USER = "test@test.com";

    @Test
    void addComment_Success() {
        CommentDto dto = new CommentDto();
        dto.setProjectId(1L);
        dto.setFileId(1L);
        dto.setContent("Hello @mentioned@test.com!");

        Comment saved = Comment.builder()
                .commentId(10L)
                .projectId(1L)
                .fileId(1L)
                .authorEmail(USER)
                .content("Hello @mentioned@test.com!")
                .build();

        when(commentRepository.save(any(Comment.class))).thenReturn(saved);

        CommentDto result = commentService.addComment(dto, USER);

        assertNotNull(result);
        assertEquals(10L, result.getCommentId());
        verify(notificationClient).sendMentionNotification("mentioned@test.com", USER, 10L, 1L);
    }

    @Test
    void getCommentsByFile_Success() {
        Comment c = Comment.builder().commentId(1L).build();
        when(commentRepository.findByFileIdAndParentCommentIdIsNullOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(c));

        var list = commentService.getCommentsByFile(1L);
        assertFalse(list.isEmpty());
    }

    @Test
    void getCommentById_Found() {
        Comment c = Comment.builder().commentId(1L).build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(c));

        var dto = commentService.getCommentById(1L);
        assertNotNull(dto);
        assertEquals(1L, dto.getCommentId());
    }

    @Test
    void updateComment_Success() {
        Comment c = Comment.builder().commentId(1L).authorEmail(USER).build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(c));
        when(commentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var dto = commentService.updateComment(1L, "New content", USER);
        assertEquals("New content", dto.getContent());
    }

    @Test
    void deleteComment_Success() {
        Comment c = Comment.builder().commentId(1L).authorEmail(USER).build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(c));

        commentService.deleteComment(1L, USER);
        verify(commentRepository).delete(c);
    }

    @Test
    void resolveComment_Success() {
        Comment c = Comment.builder().commentId(1L).authorEmail(USER).resolved(false).build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(c));
        when(commentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var dto = commentService.resolveComment(1L, USER);
        assertTrue(dto.getResolved());
    }
}
