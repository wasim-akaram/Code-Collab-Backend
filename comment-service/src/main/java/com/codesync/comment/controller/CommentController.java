/*
 * Code reader note: Exposes REST endpoints for creating, listing, updating,
 * deleting, resolving, and unresolving code comments and replies.
 * Annotations used: @RestController publishes the API, @RequestMapping sets the
 * /comments base path, @RequiredArgsConstructor injects the service, and the
 * mapping annotations bind each HTTP route. @Valid enforces request validation.
 */
package com.codesync.comment.controller;

import com.codesync.comment.dto.CommentDto;
import com.codesync.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Code Review Comments.
 * X-User header contains the user's email (set by the API Gateway).
 */
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /** Add a new comment or reply. */
    @PostMapping
    public ResponseEntity<CommentDto> addComment(@Valid @RequestBody CommentDto dto,
                                                 @RequestHeader("X-User") String userEmail) {
        return new ResponseEntity<>(commentService.addComment(dto, userEmail), HttpStatus.CREATED);
    }

    /** Get all top-level comments for a file. */
    @GetMapping("/file/{fileId}")
    public ResponseEntity<List<CommentDto>> getCommentsByFile(@PathVariable Long fileId) {
        return ResponseEntity.ok(commentService.getCommentsByFile(fileId));
    }

    /** Get all comments (including replies) for a project. */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<CommentDto>> getCommentsByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(commentService.getCommentsByProject(projectId));
    }

    /** Get a single comment by ID. */
    @GetMapping("/{commentId}")
    public ResponseEntity<CommentDto> getCommentById(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.getCommentById(commentId));
    }

    /** Get replies for a specific parent comment. */
    @GetMapping("/{commentId}/replies")
    public ResponseEntity<List<CommentDto>> getReplies(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.getReplies(commentId));
    }

    /** Get comments anchored to a specific line of a file. */
    @GetMapping("/file/{fileId}/line/{lineNumber}")
    public ResponseEntity<List<CommentDto>> getCommentsByLine(@PathVariable Long fileId,
                                                              @PathVariable Integer lineNumber) {
        return ResponseEntity.ok(commentService.getCommentsByLine(fileId, lineNumber));
    }

    /** Get total comment count for a file. */
    @GetMapping("/file/{fileId}/count")
    public ResponseEntity<Long> getCommentCount(@PathVariable Long fileId) {
        return ResponseEntity.ok(commentService.getCommentCount(fileId));
    }

    /** Update comment content (author only). */
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentDto> updateComment(@PathVariable Long commentId,
                                                    @RequestBody Map<String, String> body,
                                                    @RequestHeader("X-User") String userEmail) {
        String content = body.get("content");
        return ResponseEntity.ok(commentService.updateComment(commentId, content, userEmail));
    }

    /** Delete a comment (author only). */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId,
                                              @RequestHeader("X-User") String userEmail) {
        commentService.deleteComment(commentId, userEmail);
        return ResponseEntity.noContent().build();
    }

    /** Resolve a comment thread. */
    @PostMapping("/{commentId}/resolve")
    public ResponseEntity<CommentDto> resolveComment(@PathVariable Long commentId,
                                                     @RequestHeader("X-User") String userEmail) {
        return ResponseEntity.ok(commentService.resolveComment(commentId, userEmail));
    }

    /** Unresolve a comment thread. */
    @PostMapping("/{commentId}/unresolve")
    public ResponseEntity<CommentDto> unresolveComment(@PathVariable Long commentId,
                                                       @RequestHeader("X-User") String userEmail) {
        return ResponseEntity.ok(commentService.unresolveComment(commentId, userEmail));
    }
}
