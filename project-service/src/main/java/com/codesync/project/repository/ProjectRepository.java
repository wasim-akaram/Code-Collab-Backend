/*
 * Code reader note: Provides Spring Data queries for finding, filtering, searching, and ranking projects.
 */
package com.codesync.project.repository;

import com.codesync.project.entity.Project;
import com.codesync.common.enums.ProjectVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    // ✅ Find projects by owner email
    Page<Project> findByOwnerEmail(String ownerEmail, Pageable pageable);

    // ✅ Find non-archived projects by owner
    Page<Project> findByOwnerEmailAndArchivedFalse(String ownerEmail, Pageable pageable);

    // ✅ Find archived projects by owner
    Page<Project> findByOwnerEmailAndArchivedTrue(String ownerEmail, Pageable pageable);


    // ✅ Public projects
    Page<Project> findByVisibility(ProjectVisibility visibility, Pageable pageable);

    // ✅ Check duplicate project
    boolean existsByNameAndOwnerEmail(String name, String ownerEmail);

    // ✅ Search projects (FIXED query precedence)
    @Query("SELECT p FROM Project p WHERE " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND p.visibility = :visibility")
    Page<Project> searchByNameOrDescription(
            @Param("searchTerm") String searchTerm,
            @Param("visibility") ProjectVisibility visibility,
            Pageable pageable);

    // ✅ Find by language
    Page<Project> findByLanguage(String language, Pageable pageable);

    // ✅ Forked projects
    List<Project> findByParentProjectId(Long parentProjectId);

    // ✅ Trending
    @Query("SELECT p FROM Project p WHERE p.visibility = :visibility " +
           "ORDER BY p.starCount DESC")
    Page<Project> findTrendingProjects(
            @Param("visibility") ProjectVisibility visibility,
            Pageable pageable);

    // ─── Admin stats helpers ──────────────────────────────────────────────────
    long countByArchivedTrue();
    long countByVisibility(ProjectVisibility visibility);

    // ✅ Filter by language — public projects OR owner's own projects (case-insensitive)
    @Query("SELECT p FROM Project p WHERE " +
           "LOWER(p.language) = LOWER(:language) AND " +
           "(p.visibility = com.codesync.common.enums.ProjectVisibility.PUBLIC OR p.ownerEmail = :ownerEmail) " +
           "AND p.archived = false")
    Page<Project> findByLanguageForUser(
            @Param("language") String language,
            @Param("ownerEmail") String ownerEmail,
            Pageable pageable);
}