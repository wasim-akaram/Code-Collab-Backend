/*
 * Code reader note: Provides database queries for CodeFile records by project, path, deletion state, and content search.
 */
package com.codesync.file.repository;

import com.codesync.file.entity.CodeFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for CodeFile entity.
 * Provides standard CRUD and custom search operations.
 */
@Repository
public interface FileRepository extends JpaRepository<CodeFile, Long> {

    /** Find all active (non-deleted) files/folders for a given project. */
    List<CodeFile> findByProjectIdAndIsDeletedFalse(Long projectId);

    /** Find a specific active file in a project by its full path. */
    Optional<CodeFile> findByProjectIdAndPathAndIsDeletedFalse(Long projectId, String path);

    /** Check if a path already exists for this project (active files only). */
    boolean existsByProjectIdAndPathAndIsDeletedFalse(Long projectId, String path);

    /** Find all deleted files for a project (trash / restoration). */
    List<CodeFile> findByProjectIdAndIsDeletedTrue(Long projectId);

    /** Count the number of active files in a project. */
    long countByProjectIdAndIsDeletedFalse(Long projectId);

    /** Find active files by programming language. */
    List<CodeFile> findByLanguageAndIsDeletedFalse(String language);

    /** Find active files last edited by a specific user (email). */
    List<CodeFile> findByLastEditedByAndIsDeletedFalse(String lastEditedBy);

    /** Find all deleted files globally (admin use). */
    List<CodeFile> findByIsDeletedTrue();

    /**
     * Search within a project: match file name or content (case-insensitive).
     */
    @Query("SELECT f FROM CodeFile f WHERE f.projectId = :projectId " +
           "AND f.isDeleted = false " +
           "AND (LOWER(f.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(f.content) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<CodeFile> searchInProject(@Param("projectId") Long projectId,
                                   @Param("query") String query);

    /** Find all active files whose path starts with a given prefix (for cascading folder ops). */
    @Query("SELECT f FROM CodeFile f WHERE f.projectId = :projectId " +
           "AND f.isDeleted = false AND f.path LIKE CONCAT(:prefix, '%')")
    List<CodeFile> findByProjectIdAndPathStartingWith(@Param("projectId") Long projectId,
                                                      @Param("prefix") String prefix);
}
