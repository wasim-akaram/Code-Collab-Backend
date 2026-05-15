package com.codesync.project.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.codesync.common.enums.ProjectVisibility;
import com.codesync.project.client.NotificationClient;
import com.codesync.project.dto.CreateProjectRequest;
import com.codesync.project.dto.ProjectDto;
import com.codesync.project.entity.Project;
import com.codesync.project.entity.ProjectMember;
import com.codesync.project.repository.ProjectMemberRepository;
import com.codesync.project.repository.ProjectRepository;

/** Unit tests for {@link ProjectServiceImpl}. */
@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock private ProjectRepository projectRepo;
    @Mock private ProjectMemberRepository memberRepo;
    @Mock private NotificationClient notificationClient;
    @InjectMocks private ProjectServiceImpl service;

    private static final String USER = "owner@test.com";
    private final Pageable pageable = PageRequest.of(0, 20);

    @BeforeEach
    void setUpAuth() {
        var auth = new UsernamePasswordAuthenticationToken(
                USER, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        auth.setDetails(Map.of("plan", "PRO"));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private Project buildProject(Long id, String name) {
        Project p = Project.builder()
                .name(name).ownerEmail(USER)
                .visibility(ProjectVisibility.PUBLIC)
                .archived(false).starCount(0L).forkCount(0L)
                .defaultBranch("main").build();
        p.setId(id);
        return p;
    }

    @Test @DisplayName("createProject should save and return DTO")
    void createProject_success() {
        CreateProjectRequest req = new CreateProjectRequest();
        req.setName("TestProj");
        req.setDescription("Desc");
        req.setLanguage("Java");
        req.setVisibility(ProjectVisibility.PUBLIC);

        when(projectRepo.existsByNameAndOwnerEmail("TestProj", USER)).thenReturn(false);
        when(projectRepo.save(any())).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProjectDto dto = service.createProject(req);
        assertNotNull(dto);
        assertEquals("TestProj", dto.getName());
        verify(projectRepo).save(any());
    }

    @Test @DisplayName("createProject duplicate name should throw")
    void createProject_duplicate() {
        CreateProjectRequest req = new CreateProjectRequest();
        req.setName("Dup");
        when(projectRepo.existsByNameAndOwnerEmail("Dup", USER)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> service.createProject(req));
    }

    @Test @DisplayName("createProject FREE plan exceeds limit should throw")
    void createProject_freePlanLimit() {
        // Set FREE plan
        var auth = new UsernamePasswordAuthenticationToken(
                USER, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        auth.setDetails(Map.of("plan", "FREE"));
        SecurityContextHolder.getContext().setAuthentication(auth);

        CreateProjectRequest req = new CreateProjectRequest();
        req.setName("New");
        req.setVisibility(ProjectVisibility.PUBLIC);
        when(projectRepo.existsByNameAndOwnerEmail("New", USER)).thenReturn(false);
        when(projectRepo.countByOwnerEmailAndArchivedFalse(USER)).thenReturn(5L);

        assertThrows(RuntimeException.class, () -> service.createProject(req));
    }

    @Test @DisplayName("getProjectById found should return DTO")
    void getProjectById_found() {
        Project p = buildProject(1L, "P1");
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));

        ProjectDto dto = service.getProjectById(1L);
        assertEquals("P1", dto.getName());
    }

    @Test @DisplayName("getProjectById not found should throw")
    void getProjectById_notFound() {
        when(projectRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.getProjectById(99L));
    }

    @Test @DisplayName("getUserProjects should return paginated response")
    void getUserProjects() {
        Page<Project> page = new PageImpl<>(List.of(buildProject(1L, "P1")));
        when(projectRepo.findUserOwnedOrMemberProjects(USER, pageable)).thenReturn(page);

        var result = service.getUserProjects(pageable);
        assertEquals(1, result.getContent().size());
    }

    @Test @DisplayName("getArchivedProjects returns archived projects")
    void getArchivedProjects() {
        when(projectRepo.findByOwnerEmailAndArchivedTrue(USER, pageable))
                .thenReturn(new PageImpl<>(List.of()));
        var result = service.getArchivedProjects(pageable);
        assertTrue(result.getContent().isEmpty());
    }

    @Test @DisplayName("getPublicProjects returns public projects")
    void getPublicProjects() {
        when(projectRepo.findByVisibility(ProjectVisibility.PUBLIC, pageable))
                .thenReturn(new PageImpl<>(List.of()));
        var result = service.getPublicProjects(pageable);
        assertNotNull(result);
    }

    @Test @DisplayName("deleteProject owner should succeed")
    void deleteProject_owner() {
        Project p = buildProject(1L, "P1");
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));
        doNothing().when(projectRepo).delete(p);

        assertDoesNotThrow(() -> service.deleteProject(1L));
    }

    @Test @DisplayName("deleteProject non-owner should throw")
    void deleteProject_nonOwner() {
        Project p = buildProject(1L, "P1");
        p.setOwnerEmail("other@test.com");
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));

        assertThrows(RuntimeException.class, () -> service.deleteProject(1L));
    }

    @Test @DisplayName("updateProject should update fields")
    void updateProject() {
        Project p = buildProject(1L, "Old");
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));
        when(projectRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateProjectRequest req = new CreateProjectRequest();
        req.setName("New");
        req.setDescription("Updated");
        req.setLanguage("Python");
        req.setVisibility(ProjectVisibility.PRIVATE);

        ProjectDto dto = service.updateProject(1L, req);
        assertEquals("New", dto.getName());
    }

    @Test @DisplayName("archiveProject should set archived true")
    void archiveProject() {
        Project p = buildProject(1L, "P1");
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));
        when(projectRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProjectDto dto = service.archiveProject(1L);
        assertNotNull(dto);
        assertTrue(dto.getArchived());
    }

    @Test @DisplayName("starProject should increment star count")
    void starProject() {
        Project p = buildProject(1L, "P1");
        p.setStarCount(0L);
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));
        when(projectRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.starProject(1L);
        verify(projectRepo).save(argThat(proj -> proj.getStarCount() == 1L));
    }

    @Test @DisplayName("unstarProject should decrement star count")
    void unstarProject() {
        Project p = buildProject(1L, "P1");
        p.setStarCount(5L);
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));
        when(projectRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.unstarProject(1L);
        verify(projectRepo).save(argThat(proj -> proj.getStarCount() == 4L));
    }

    @Test @DisplayName("canEditProject owner should return true")
    void canEditProject_owner() {
        Project p = buildProject(1L, "P1");
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));

        assertTrue(service.canEditProject(1L, USER));
    }

    @Test @DisplayName("canEditProject non-member should return false")
    void canEditProject_nonMember() {
        Project p = buildProject(1L, "P1");
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));
        when(memberRepo.findByProjectIdAndUserEmail(1L, "other@t.com")).thenReturn(Optional.empty());

        assertFalse(service.canEditProject(1L, "other@t.com"));
    }

    @Test @DisplayName("addMember should save project member")
    void addMember() {
        Project p = buildProject(1L, "P1");
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));
        when(memberRepo.existsByProjectIdAndUserEmail(1L, "new@t.com")).thenReturn(false);
        when(memberRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> service.addMember(1L, "new@t.com", "EDITOR"));
        verify(memberRepo).save(any());
    }

    @Test @DisplayName("projectExists should return true when exists")
    void projectExists() {
        when(projectRepo.existsById(1L)).thenReturn(true);
        assertTrue(service.projectExists(1L));
    }

    @Test @DisplayName("getUser not authenticated should throw")
    void getUser_notAuthenticated() {
        SecurityContextHolder.clearContext();
        assertThrows(RuntimeException.class, () -> service.createProject(new CreateProjectRequest()));
    }

    @Test @DisplayName("searchProjects delegates to repo")
    void searchProjects() {
        when(projectRepo.searchByNameOrDescription("test", ProjectVisibility.PUBLIC, pageable))
                .thenReturn(new PageImpl<>(List.of()));
        var result = service.searchProjects("test", ProjectVisibility.PUBLIC, pageable);
        assertNotNull(result);
    }

    @Test @DisplayName("forkProject should duplicate project and save")
    void forkProject() {
        Project p = buildProject(1L, "Original");
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));
        when(projectRepo.save(any())).thenAnswer(inv -> {
            Project f = inv.getArgument(0);
            f.setId(2L);
            return f;
        });

        ProjectDto dto = service.forkProject(1L);
        assertNotNull(dto);
        assertEquals("Original-fork", dto.getName());
        verify(notificationClient).sendForkNotification(eq(USER), eq(USER), eq("Original"));
    }

    @Test @DisplayName("getProjectMembers should return members")
    void getProjectMembers() {
        when(memberRepo.findByProjectId(eq(1L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(new ProjectMember())));
        var members = service.getProjectMembers(1L, pageable);
        assertFalse(members.getContent().isEmpty());
    }

    @Test @DisplayName("removeMember should delete member")
    void removeMember() {
        Project p = buildProject(1L, "P1");
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));
        when(memberRepo.existsByProjectIdAndUserEmail(1L, "member@t.com")).thenReturn(true);

        assertDoesNotThrow(() -> service.removeMember(1L, "member@t.com"));
        verify(memberRepo).deleteByProjectIdAndUserEmail(1L, "member@t.com");
    }

    @Test @DisplayName("admin force delete project should succeed")
    void forceDeleteProject() {
        Project p = buildProject(1L, "P1");
        when(projectRepo.findById(1L)).thenReturn(Optional.of(p));

        service.forceDeleteProject(1L);
        verify(projectRepo).delete(p);
    }
}
