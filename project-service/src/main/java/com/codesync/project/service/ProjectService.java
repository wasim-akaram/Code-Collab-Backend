/*
 * Code reader note: Defines the project management operations implemented by the service layer.
 */
package com.codesync.project.service;

import com.codesync.common.enums.ProjectVisibility;
import com.codesync.common.dto.PageResponse;
import com.codesync.project.dto.CreateProjectRequest;
import com.codesync.project.dto.ProjectDto;
import com.codesync.project.dto.ProjectMemberDto;
import org.springframework.data.domain.Pageable;

public interface ProjectService {

    // Create new project
    ProjectDto createProject(CreateProjectRequest request);

    // Get project by ID
    ProjectDto getProjectById(Long projectId);

    // Get all projects of logged-in user
    PageResponse<ProjectDto> getUserProjects(Pageable pageable);

    // Get archived projects of logged-in user
    PageResponse<ProjectDto> getArchivedProjects(Pageable pageable);

    // Get public projects
    PageResponse<ProjectDto> getPublicProjects(Pageable pageable);

    // Search projects
    PageResponse<ProjectDto> searchProjects(String searchTerm, ProjectVisibility visibility, Pageable pageable);

    // Update project
    ProjectDto updateProject(Long projectId, CreateProjectRequest request);

    // Delete project
    void deleteProject(Long projectId);

    // Archive project
    ProjectDto archiveProject(Long projectId);

    // Star project
    void starProject(Long projectId);

    // Unstar project
    void unstarProject(Long projectId);

    // Fork project
    ProjectDto forkProject(Long projectId);

    // Add member to project
    void addMember(Long projectId, String userEmail, String role);

    // Remove member from project
    void removeMember(Long projectId, String userEmail);

    // Get project members
    PageResponse<ProjectMemberDto> getProjectMembers(Long projectId, Pageable pageable);

    // Check if user is project member
    boolean isProjectMember(Long projectId, String userEmail);

    // Check if user can edit a project (owner or editor role)
    boolean canEditProject(Long projectId, String userEmail);

    // Check if user is owner of project
    boolean isProjectOwner(Long projectId, String userEmail);

    // Check if project exists
    boolean projectExists(Long projectId);

    // Get trending projects
    PageResponse<ProjectDto> getTrendingProjects(Pageable pageable);

    // Filter by language — public + user's own projects
    PageResponse<ProjectDto> getProjectsByLanguage(String language, Pageable pageable);

    // ─── Admin-only operations ────────────────────────────────────────────────

    /** Returns ALL projects across all users (admin view, paginated). */
    PageResponse<ProjectDto> getAllProjectsAdmin(Pageable pageable);

    /** Force-deletes any project regardless of ownership (admin only). */
    void forceDeleteProject(Long projectId);

    /** Returns platform-wide project stats for the admin dashboard. */
    java.util.Map<String, Long> getAdminProjectStats();
}