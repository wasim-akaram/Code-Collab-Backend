/*
 * Code reader note: Implements project business rules for ownership, visibility,
 * pagination, starring, forking, archiving, membership, and admin actions.
 * Annotations used: @Service registers the business service, @Transactional wraps
 * the class in transactions, and @RequiredArgsConstructor injects the repositories
 * and notification client.
 */
package com.codesync.project.service.impl;

import com.codesync.common.dto.PageResponse;
import com.codesync.common.enums.ProjectVisibility;
import com.codesync.project.client.NotificationClient;
import com.codesync.project.dto.CreateProjectRequest;
import com.codesync.project.dto.ProjectDto;
import com.codesync.project.dto.ProjectMemberDto;
import com.codesync.project.entity.Project;
import com.codesync.project.entity.ProjectMember;
import com.codesync.project.repository.ProjectMemberRepository;
import com.codesync.project.repository.ProjectRepository;
import com.codesync.project.service.ProjectService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of ProjectService.
 * Handles business logic for project creation, retrieval, updating, and deletion.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final NotificationClient notificationClient;

    /**
     * Extracts the email of the currently authenticated user from the SecurityContext.
     * @return The user's email.
     * @throws RuntimeException if authentication is missing or invalid.
     */
    private String getUser() {
        // JwtFilter sets the authenticated principal to the user's email. All
        // owner/member checks in this service depend on that value.
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        return auth.getPrincipal().toString();
    }

    /**
     * Reads the user's subscription plan from the SecurityContext details.
     * Returns "FREE" if not set.
     */
    @SuppressWarnings("unchecked")
    private String getUserPlan() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof java.util.Map) {
            var details = (java.util.Map<String, String>) auth.getDetails();
            String plan = details.get("plan");
            return (plan != null && !plan.isBlank()) ? plan : "FREE";
        }
        return "FREE";
    }

    // ─── Plan limits ────────────────────────────────────────────────────────────
    private static final int FREE_MAX_PROJECTS = 5;
    private static final int FREE_MAX_PRIVATE_PROJECTS = 1;

    /**
     * Creates a new project with the provided details.
     * Enforces plan-based limits: FREE users get 5 total projects, 1 private project.
     * @param request Data transfer object containing the project details.
     * @return The created ProjectDto.
     */
    @Override
    public ProjectDto createProject(CreateProjectRequest request) {

        String email = getUser();
        String plan = getUserPlan();

        // Project names only need to be unique per owner, so two different users
        // can still create projects with the same display name.
        if (projectRepository.existsByNameAndOwnerEmail(request.getName(), email)) {
            throw new RuntimeException("A project with name '" + request.getName() + "' already exists.");
        }

        // ── Plan-based limits (FREE tier only) ──────────────────────────────
        if (!"PRO".equalsIgnoreCase(plan)) {
            long totalProjects = projectRepository.countByOwnerEmailAndArchivedFalse(email);
            if (totalProjects >= FREE_MAX_PROJECTS) {
                throw new RuntimeException(
                        "Free plan limit reached: you can have up to " + FREE_MAX_PROJECTS +
                        " projects. Upgrade to Pro for unlimited projects.");
            }

            if (request.getVisibility() == ProjectVisibility.PRIVATE) {
                long privateProjects = projectRepository.countByOwnerEmailAndVisibilityAndArchivedFalse(
                        email, ProjectVisibility.PRIVATE);
                if (privateProjects >= FREE_MAX_PRIVATE_PROJECTS) {
                    throw new RuntimeException(
                            "Free plan limit reached: you can have up to " + FREE_MAX_PRIVATE_PROJECTS +
                            " private project. Upgrade to Pro for unlimited private projects.");
                }
            }
        }

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .ownerEmail(email)
                .language(request.getLanguage())
                .visibility(request.getVisibility())
                .archived(false)
                .starCount(0L)
                .forkCount(0L)
                .defaultBranch("main")
                .build();

        return map(projectRepository.save(project));
    }

    /**
     * Retrieves a project by its ID.
     * @param projectId The ID of the project.
     * @return The ProjectDto.
     * @throws RuntimeException if the project is not found.
     */
    @Override
    public ProjectDto getProjectById(Long projectId) {
        return map(projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found")));
    }

    /**
     * Retrieves all non-archived projects owned by OR shared with the currently authenticated user.
     * @param pageable Pagination parameters.
     * @return Paginated response containing user's own + member projects.
     */
    @Override
    public PageResponse<ProjectDto> getUserProjects(Pageable pageable) {
        String email = getUser();
        Page<Project> page = projectRepository.findUserOwnedOrMemberProjects(email, pageable);
        return buildPage(page);
    }

    @Override
    public PageResponse<ProjectDto> getArchivedProjects(Pageable pageable) {
        String email = getUser();
        Page<Project> page = projectRepository.findByOwnerEmailAndArchivedTrue(email, pageable);
        return buildPage(page);
    }

    /**
     * Retrieves all public projects.
     * @param pageable Pagination parameters.
     * @return Paginated response containing public projects.
     */
    @Override
    public PageResponse<ProjectDto> getPublicProjects(Pageable pageable) {

        Page<Project> page =
                projectRepository.findByVisibility(ProjectVisibility.PUBLIC, pageable);

        return buildPage(page);
    }

    /**
     * Updates an existing project.
     * @param id The ID of the project to update.
     * @param request The updated project details.
     * @return The updated ProjectDto.
     * @throws RuntimeException if the project is not found or the user is not the owner.
     */
    @Override
    public ProjectDto updateProject(Long id, CreateProjectRequest request) {

        String email = getUser();

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getOwnerEmail().equals(email)) {
            throw new RuntimeException("Not owner");
        }

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setLanguage(request.getLanguage());
        project.setVisibility(request.getVisibility());

        return map(projectRepository.save(project));
    }

    /**
     * Deletes an existing project.
     * @param id The ID of the project to delete.
     * @throws RuntimeException if the project is not found or the user is not the owner.
     */
    @Override
    public void deleteProject(Long id) {

        String email = getUser();

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getOwnerEmail().equals(email)) {
            throw new RuntimeException("Not owner");
        }

        projectRepository.delete(project);
    }

    /**
     * Maps a Project entity to a ProjectDto.
     */
    private ProjectDto map(Project p) {
        return ProjectDto.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .ownerEmail(p.getOwnerEmail())
                .language(p.getLanguage())
                .visibility(p.getVisibility())
                .archived(p.getArchived())
                .starCount(p.getStarCount())
                .forkCount(p.getForkCount())
                .parentProjectId(p.getParentProjectId())
                .defaultBranch(p.getDefaultBranch())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    /**
     * Builds a paginated response wrapper from a Spring Data Page object.
     */
    private PageResponse<ProjectDto> buildPage(Page<Project> page) {
        return PageResponse.<ProjectDto>builder()
                .content(page.getContent().stream().map(this::map).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }

    // Remaining methods (not implemented yet)

    @Override
    public PageResponse<ProjectDto> searchProjects(String searchTerm, ProjectVisibility visibility, Pageable pageable) {
        // The repository combines text search with an optional visibility filter
        // while Pageable keeps large result sets split into pages.
        Page<Project> page = projectRepository.searchByNameOrDescription(searchTerm, visibility, pageable);
        return buildPage(page);
    }

    @Override
    public ProjectDto archiveProject(Long id) {
        String email = getUser();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getOwnerEmail().equals(email)) {
            throw new RuntimeException("Not owner");
        }

        project.setArchived(true);
        return map(projectRepository.save(project));
    }

    @Override
    public void starProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        project.setStarCount(project.getStarCount() + 1);
        projectRepository.save(project);
    }

    @Override
    public void unstarProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        if (project.getStarCount() > 0) {
            project.setStarCount(project.getStarCount() - 1);
            projectRepository.save(project);
        }
    }

    @Override
    public ProjectDto forkProject(Long id) {
        String email = getUser();
        Project original = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // A fork starts as a private project owned by the current user and keeps a
        // parentProjectId so the original/fork relationship can be shown later.
        Project forked = Project.builder()
                .name(original.getName() + "-fork")
                .description(original.getDescription())
                .ownerEmail(email)
                .language(original.getLanguage())
                .visibility(ProjectVisibility.PRIVATE)
                .archived(false)
                .starCount(0L)
                .forkCount(0L)
                .parentProjectId(original.getId())
                .defaultBranch(original.getDefaultBranch())
                .build();

        original.setForkCount(original.getForkCount() + 1);
        projectRepository.save(original);

        Project saved = projectRepository.save(forked);

        // Notify original owner that their project was forked
        notificationClient.sendForkNotification(original.getOwnerEmail(), email, original.getName());

        return map(saved);
    }

    @Override
    public PageResponse<ProjectDto> getTrendingProjects(Pageable pageable) {
        Page<Project> page = projectRepository.findTrendingProjects(ProjectVisibility.PUBLIC, pageable);
        return buildPage(page);
    }

    @Override
    public PageResponse<ProjectDto> getProjectsByLanguage(String language, Pageable pageable) {
        String email = getUser();
        // This query intentionally returns public projects plus private projects
        // owned by the current user, so language filters do not leak private work.
        Page<Project> page = projectRepository.findByLanguageForUser(language, email, pageable);
        return buildPage(page);
    }

    @Override
    public boolean projectExists(Long id) {
        return projectRepository.existsById(id);
    }

    @Override
    public boolean isProjectOwner(Long id, String email) {
        return projectRepository.findById(id)
                .map(p -> p.getOwnerEmail().equals(email))
                .orElse(false);
    }

    // ─── Member Management ───────────────────────────────────────────────────────

    @Override
    public void addMember(Long projectId, String userEmail, String role) {
        String requester = getUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        if (!project.getOwnerEmail().equals(requester)) {
            throw new RuntimeException("Only the project owner can add members");
        }
        if (memberRepository.existsByProjectIdAndUserEmail(projectId, userEmail)) {
            throw new RuntimeException("User is already a member of this project");
        }
        // Missing role defaults to the least-privileged collaboration role.
        String resolvedRole = (role != null && !role.isBlank()) ? role.toUpperCase() : "VIEWER";
        memberRepository.save(ProjectMember.builder()
                .projectId(projectId)
                .userEmail(userEmail)
                .role(resolvedRole)
                .build());

        // Notify the new member that they were added
        notificationClient.sendMemberAddedNotification(userEmail, requester, project.getName(), resolvedRole);
    }

    @Override
    @Transactional
    public void removeMember(Long projectId, String userEmail) {
        String requester = getUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        if (!project.getOwnerEmail().equals(requester)) {
            throw new RuntimeException("Only the project owner can remove members");
        }
        if (!memberRepository.existsByProjectIdAndUserEmail(projectId, userEmail)) {
            throw new RuntimeException("User is not a member of this project");
        }
        memberRepository.deleteByProjectIdAndUserEmail(projectId, userEmail);
    }

    @Override
    public PageResponse<ProjectMemberDto> getProjectMembers(Long projectId, Pageable pageable) {
        Page<ProjectMember> page = memberRepository.findByProjectId(projectId, pageable);
        // Project members use the same generic PageResponse shape as project
        // listings, which keeps frontend pagination code consistent.
        return PageResponse.<ProjectMemberDto>builder()
                .content(page.getContent().stream().map(this::mapMember).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }

    @Override
    public boolean isProjectMember(Long projectId, String userEmail) {
        return memberRepository.existsByProjectIdAndUserEmail(projectId, userEmail);
    }

    @Override
    public boolean canEditProject(Long projectId, String userEmail) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return false;
        }

        if (project.getOwnerEmail().equals(userEmail)) {
            return true;
        }

        return memberRepository.findByProjectIdAndUserEmail(projectId, userEmail)
                .map(member -> {
                    String role = member.getRole() == null ? "" : member.getRole().trim().toUpperCase();
                    return "EDITOR".equals(role) || "OWNER".equals(role);
                })
                .orElse(false);
    }

    private ProjectMemberDto mapMember(ProjectMember m) {
        return ProjectMemberDto.builder()
                .id(m.getId())
                .projectId(m.getProjectId())
                .userEmail(m.getUserEmail())
                .role(m.getRole())
                .createdAt(m.getCreatedAt())
                .build();
    }

    // ─── Admin-only operations ───────────────────────────────────────────────────

    @Override
    public PageResponse<ProjectDto> getAllProjectsAdmin(Pageable pageable) {
        Page<Project> page = projectRepository.findAll(pageable);
        return buildPage(page);
    }

    @Override
    public void forceDeleteProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        projectRepository.delete(project);
    }

    @Override
    public java.util.Map<String, Long> getAdminProjectStats() {
        long total    = projectRepository.count();
        long archived = projectRepository.countByArchivedTrue();
        long pub      = projectRepository.countByVisibility(ProjectVisibility.PUBLIC);
        return java.util.Map.of(
                "totalProjects",   total,
                "archivedProjects", archived,
                "publicProjects",   pub
        );
    }

}
