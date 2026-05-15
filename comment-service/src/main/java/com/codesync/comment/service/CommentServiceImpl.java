/*
 * Code reader note: Implements comment creation, replies, line/project/file lookups, author-checked edits/deletes, resolve state, and mention notifications.
 */
package com.codesync.comment.service;

import com.codesync.comment.client.NotificationClient;
import com.codesync.comment.dto.CommentDto;
import com.codesync.comment.entity.Comment;
import com.codesync.comment.exception.ResourceNotFoundException;
import com.codesync.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementation of CommentService.
 * Parses @mention tags in comment content and triggers async notifications
 * to the notification-service for each mentioned user.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final NotificationClient notificationClient;

    // Matches @email patterns in comment text, e.g., @user@example.com
    // The escaped hyphen keeps '-' literal inside the character class.
    private static final Pattern MENTION_PATTERN =
            Pattern.compile("@([a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,})");

    @Override
    @Transactional
    public CommentDto addComment(CommentDto dto, String authorEmail) {
        // parentCommentId being present means this record is a reply; null means
        // this is a top-level comment thread.
        Comment comment = Comment.builder()
                .projectId(dto.getProjectId())
                .fileId(dto.getFileId())
                .authorEmail(authorEmail)
                .content(dto.getContent())
                .lineNumber(dto.getLineNumber())
                .columnNumber(dto.getColumnNumber())
                .parentCommentId(dto.getParentCommentId())
                .snapshotId(dto.getSnapshotId())
                .build();

        Comment saved = commentRepository.save(comment);

        // Parse and dispatch @mention notifications (async, non-blocking)
        dispatchMentionNotifications(dto.getContent(), authorEmail, saved.getCommentId(), dto.getFileId());

        return mapToDto(saved);
    }

    @Override
    public List<CommentDto> getCommentsByFile(Long fileId) {
        return commentRepository.findByFileIdAndParentCommentIdIsNullOrderByCreatedAtAsc(fileId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getCommentsByProject(Long projectId) {
        return commentRepository.findByProjectId(projectId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public CommentDto getCommentById(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));
        return mapToDto(comment);
    }

    @Override
    public List<CommentDto> getReplies(Long parentCommentId) {
        return commentRepository.findByParentCommentIdOrderByCreatedAtAsc(parentCommentId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getCommentsByLine(Long fileId, Integer lineNumber) {
        return commentRepository.findByFileIdAndLineNumber(fileId, lineNumber)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public long getCommentCount(Long fileId) {
        return commentRepository.countByFileId(fileId);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long commentId, String content, String userEmail) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        if (!comment.getAuthorEmail().equals(userEmail)) {
            throw new SecurityException("Only the author can edit this comment");
        }
        // Updating content can introduce new mentions, so mention parsing runs
        // again after edits.
        comment.setContent(content);

        // Dispatch mention notifications for the updated content too
        dispatchMentionNotifications(content, userEmail, commentId, comment.getFileId());

        return mapToDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, String userEmail) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        if (!comment.getAuthorEmail().equals(userEmail)) {
            throw new SecurityException("Only the author can delete this comment");
        }
        commentRepository.delete(comment);
    }

    @Override
    @Transactional
    public CommentDto resolveComment(Long commentId, String userEmail) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        comment.setResolved(true);
        return mapToDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public CommentDto unresolveComment(Long commentId, String userEmail) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        comment.setResolved(false);
        return mapToDto(commentRepository.save(comment));
    }

    // ─── Mention parsing ─────────────────────────────────────────────────────

    /**
     * Scans comment content for @email patterns and fires an async notification
     * to the notification-service for each mentioned user.
     * Skips the author's own email to avoid self-notifications.
     */
    private void dispatchMentionNotifications(String content, String authorEmail,
                                               Long commentId, Long fileId) {
        if (content == null || content.isBlank()) return;

        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            // Group 1 is the email inside @email, excluding the leading @.
            String mentionedEmail = matcher.group(1);
            if (!mentionedEmail.equalsIgnoreCase(authorEmail)) {
                log.info("Dispatching mention notification to {} from comment #{}", mentionedEmail, commentId);
                notificationClient.sendMentionNotification(mentionedEmail, authorEmail, commentId, fileId);
            }
        }
    }

    // ─── Mapping ─────────────────────────────────────────────────────────────

    private CommentDto mapToDto(Comment comment) {
        return CommentDto.builder()
                .commentId(comment.getCommentId())
                .projectId(comment.getProjectId())
                .fileId(comment.getFileId())
                .authorEmail(comment.getAuthorEmail())
                .content(comment.getContent())
                .lineNumber(comment.getLineNumber())
                .columnNumber(comment.getColumnNumber())
                .parentCommentId(comment.getParentCommentId())
                .resolved(comment.getResolved())
                .snapshotId(comment.getSnapshotId())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
