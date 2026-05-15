/*
 * Code reader note: Provides database access for project membership and role records.
 */
package com.codesync.project.repository;

import com.codesync.project.entity.ProjectMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for ProjectMember entities.
 * Uses userEmail (String) to identify members — matches the email-based identity model.
 */
@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    /** All members of a project (paginated). */
    Page<ProjectMember> findByProjectId(Long projectId, Pageable pageable);

    /** Find a specific membership record. */
    Optional<ProjectMember> findByProjectIdAndUserEmail(Long projectId, String userEmail);

    /** Check if a user is a member of a project. */
    boolean existsByProjectIdAndUserEmail(Long projectId, String userEmail);

    /** All projects a user is a member of. */
    Page<ProjectMember> findByUserEmail(String userEmail, Pageable pageable);

    /** Count members in a project. */
    long countByProjectId(Long projectId);

    /** Members with a specific role in a project. */
    Page<ProjectMember> findByProjectIdAndRole(Long projectId, String role, Pageable pageable);

    /** Delete all members from a project (used when deleting the project). */
    void deleteByProjectId(Long projectId);

    /** Delete a specific member from a project. */
    void deleteByProjectIdAndUserEmail(Long projectId, String userEmail);
}
