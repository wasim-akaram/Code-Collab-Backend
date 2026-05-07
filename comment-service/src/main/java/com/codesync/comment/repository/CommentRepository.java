/*
 * Code reader note: Provides database queries for comments by file, project, parent, line, and resolved state.
 */
package com.codesync.comment.repository;

import com.codesync.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for accessing comments.
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Get all top-level comments for a file (ordered by creation time)
    List<Comment> findByFileIdAndParentCommentIdIsNullOrderByCreatedAtAsc(Long fileId);

    // Get all replies for a specific parent comment
    List<Comment> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId);

    // Get all comments for a project
    List<Comment> findByProjectId(Long projectId);

    // Get comments anchored to a specific line in a file
    List<Comment> findByFileIdAndLineNumber(Long fileId, Integer lineNumber);

    // Count all comments for a file (includes replies)
    long countByFileId(Long fileId);
}
