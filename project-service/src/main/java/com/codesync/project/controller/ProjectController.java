/*
 * Code reader note: Exposes REST endpoints for project CRUD, listing, search, archive, star, fork, member, and admin operations.
 */
package com.codesync.project.controller;

import com.codesync.common.dto.PageResponse;
import com.codesync.common.enums.ProjectVisibility;
import com.codesync.project.dto.CreateProjectRequest;
import com.codesync.project.dto.ProjectDto;
import com.codesync.project.dto.ProjectMemberDto;
import com.codesync.project.service.ProjectService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller to handle HTTP endpoints for CodeSync projects.
 */
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /** Creates a new project. */
    @PostMapping
    public ResponseEntity<ProjectDto> createProject(@RequestBody CreateProjectRequest request) {
        return ResponseEntity.ok(projectService.createProject(request));
    }

    /** Retrieves a project by ID. */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto> getProject(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    /** Retrieves all projects for the currently authenticated user. */
    @GetMapping("/my")
    public ResponseEntity<PageResponse<ProjectDto>> getMyProjects(Pageable pageable) {
        return ResponseEntity.ok(projectService.getUserProjects(pageable));
    }

    /** Retrieves archived projects for the currently authenticated user. */
    @GetMapping("/archived")
    public ResponseEntity<PageResponse<ProjectDto>> getArchivedProjects(Pageable pageable) {
        return ResponseEntity.ok(projectService.getArchivedProjects(pageable));
    }

    /** Retrieves all public projects. */
    @GetMapping("/public")
    public ResponseEntity<PageResponse<ProjectDto>> getPublicProjects(Pageable pageable) {
        return ResponseEntity.ok(projectService.getPublicProjects(pageable));
    }

    /** Retrieves trending public projects ordered by star count. */
    @GetMapping("/trending")
    public ResponseEntity<PageResponse<ProjectDto>> getTrendingProjects(Pageable pageable) {
        return ResponseEntity.ok(projectService.getTrendingProjects(pageable));
    }

    /** Filter projects by programming language — returns public + user's own projects. */
    @GetMapping("/by-language")
    public ResponseEntity<PageResponse<ProjectDto>> getProjectsByLanguage(
            @RequestParam String language,
            Pageable pageable) {
        return ResponseEntity.ok(projectService.getProjectsByLanguage(language, pageable));
    }

    /** Search public projects by name or description. */
    @GetMapping("/search")
    public ResponseEntity<PageResponse<ProjectDto>> searchProjects(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "PUBLIC") ProjectVisibility visibility,
            Pageable pageable) {
        return ResponseEntity.ok(projectService.searchProjects(searchTerm, visibility, pageable));
    }

    /** Updates an existing project. */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectDto> updateProject(
            @PathVariable Long id,
            @RequestBody CreateProjectRequest request) {
        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    /** Deletes a project. */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok("Project deleted");
    }

    /** Archives a project (soft delete — hidden from active list). */
    @PostMapping("/{id}/archive")
    public ResponseEntity<ProjectDto> archiveProject(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.archiveProject(id));
    }

    /** Forks a public project into the authenticated user's account. */
    @PostMapping("/{id}/fork")
    public ResponseEntity<ProjectDto> forkProject(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.forkProject(id));
    }

    /** Stars a project. */
    @PostMapping("/{id}/star")
    public ResponseEntity<?> star(@PathVariable Long id) {
        projectService.starProject(id);
        return ResponseEntity.ok("Starred");
    }

    /** Unstars a project. */
    @PostMapping("/{id}/unstar")
    public ResponseEntity<?> unstar(@PathVariable Long id) {
        projectService.unstarProject(id);
        return ResponseEntity.ok("Unstarred");
    }

    // ─── Member Management ────────────────────────────────────────────────────────

    /** Get all members of a project (paginated). */
    @GetMapping("/{id}/members")
    public ResponseEntity<PageResponse<ProjectMemberDto>> getMembers(
            @PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(projectService.getProjectMembers(id, pageable));
    }

    /** Internal: check whether the current user can edit the project. */
    @GetMapping("/{id}/can-edit")
    public ResponseEntity<Boolean> canEditProject(
            @PathVariable Long id,
            @RequestHeader("X-User") String userEmail) {
        return ResponseEntity.ok(projectService.canEditProject(id, userEmail));
    }

    /**
     * Add a member to a project.
     * Body: { "userEmail": "user@example.com", "role": "EDITOR" }
     */
    @PostMapping("/{id}/members")
    public ResponseEntity<Void> addMember(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String userEmail = body.get("userEmail");
        String role      = body.getOrDefault("role", "VIEWER");
        projectService.addMember(id, userEmail, role);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** Remove a member from a project. */
    @DeleteMapping("/{id}/members/{memberEmail}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id,
            @PathVariable String memberEmail) {
        projectService.removeMember(id, memberEmail);
        return ResponseEntity.noContent().build();
    }

    // ─── Admin-only endpoints ─────────────────────────────────────────────────

    /** Get all projects across all users (admin only). */
    @GetMapping("/admin/all")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ProjectDto>> getAllProjectsAdmin(Pageable pageable) {
        return ResponseEntity.ok(projectService.getAllProjectsAdmin(pageable));
    }

    /** Force-delete any project regardless of ownership (admin only). */
    @DeleteMapping("/admin/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> forceDeleteProject(@PathVariable Long id) {
        projectService.forceDeleteProject(id);
        return ResponseEntity.ok("Project deleted by admin");
    }

    /** Get platform-wide project stats (admin only). */
    @GetMapping("/admin/stats")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.Map<String, Long>> getAdminProjectStats() {
        return ResponseEntity.ok(projectService.getAdminProjectStats());
    }
}