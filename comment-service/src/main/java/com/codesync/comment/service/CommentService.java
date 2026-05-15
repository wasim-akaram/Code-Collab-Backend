/*
 * Code reader note: Defines comment thread operations implemented by the service layer.
 */
package com.codesync.comment.service;

import com.codesync.comment.dto.CommentDto;

import java.util.List;

/**
 * Interface defining comment operations.
 */
public interface CommentService {

    CommentDto addComment(CommentDto dto, String authorEmail);

    List<CommentDto> getCommentsByFile(Long fileId);

    List<CommentDto> getCommentsByProject(Long projectId);

    CommentDto getCommentById(Long commentId);

    List<CommentDto> getReplies(Long parentCommentId);

    List<CommentDto> getCommentsByLine(Long fileId, Integer lineNumber);

    long getCommentCount(Long fileId);

    CommentDto updateComment(Long commentId, String content, String userEmail);

    void deleteComment(Long commentId, String userEmail);

    CommentDto resolveComment(Long commentId, String userEmail);

    CommentDto unresolveComment(Long commentId, String userEmail);
}
