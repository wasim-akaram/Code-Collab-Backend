package com.codesync.project.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.codesync.common.dto.PageResponse;
import com.codesync.common.enums.ProjectVisibility;
import com.codesync.project.dto.CreateProjectRequest;
import com.codesync.project.dto.ProjectDto;
import com.codesync.project.dto.ProjectMemberDto;
import com.codesync.project.service.ProjectService;

/**
 * Unit tests for {@link ProjectController}.
 * Verifies each REST endpoint delegates correctly to ProjectService.
 */
@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    @Mock private ProjectService projectService;
    @InjectMocks private ProjectController controller;

    private final Pageable pageable = PageRequest.of(0, 20);

    @Test @DisplayName("createProject should delegate to service and return 200")
    void createProject() {
        CreateProjectRequest req = new CreateProjectRequest();
        ProjectDto dto = ProjectDto.builder().id(1L).name("Test").build();
        when(projectService.createProject(any())).thenReturn(dto);

        ResponseEntity<ProjectDto> resp = controller.createProject(req);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Test", resp.getBody().getName());
    }

    @Test @DisplayName("getProject should return project by ID")
    void getProject() {
        ProjectDto dto = ProjectDto.builder().id(1L).name("P1").build();
        when(projectService.getProjectById(1L)).thenReturn(dto);

        assertEquals("P1", controller.getProject(1L).getBody().getName());
    }

    @Test @DisplayName("getMyProjects should return paginated results")
    void getMyProjects() {
        PageResponse<ProjectDto> page = new PageResponse<>(List.of(), 0, 20, 0, 0, true, true);
        when(projectService.getUserProjects(pageable)).thenReturn(page);

        assertEquals(HttpStatus.OK, controller.getMyProjects(pageable).getStatusCode());
    }

    @Test @DisplayName("getArchivedProjects should return archived list")
    void getArchivedProjects() {
        PageResponse<ProjectDto> page = new PageResponse<>(List.of(), 0, 20, 0, 0, true, true);
        when(projectService.getArchivedProjects(pageable)).thenReturn(page);

        assertEquals(HttpStatus.OK, controller.getArchivedProjects(pageable).getStatusCode());
    }

    @Test @DisplayName("getPublicProjects should return public projects")
    void getPublicProjects() {
        PageResponse<ProjectDto> page = new PageResponse<>(List.of(), 0, 20, 0, 0, true, true);
        when(projectService.getPublicProjects(pageable)).thenReturn(page);

        assertEquals(HttpStatus.OK, controller.getPublicProjects(pageable).getStatusCode());
    }

    @Test @DisplayName("getTrendingProjects should return trending list")
    void getTrendingProjects() {
        PageResponse<ProjectDto> page = new PageResponse<>(List.of(), 0, 20, 0, 0, true, true);
        when(projectService.getTrendingProjects(pageable)).thenReturn(page);

        assertEquals(HttpStatus.OK, controller.getTrendingProjects(pageable).getStatusCode());
    }

    @Test @DisplayName("getProjectsByLanguage should filter by language")
    void getProjectsByLanguage() {
        PageResponse<ProjectDto> page = new PageResponse<>(List.of(), 0, 20, 0, 0, true, true);
        when(projectService.getProjectsByLanguage("Java", pageable)).thenReturn(page);

        assertEquals(HttpStatus.OK, controller.getProjectsByLanguage("Java", pageable).getStatusCode());
    }

    @Test @DisplayName("searchProjects should search by term")
    void searchProjects() {
        PageResponse<ProjectDto> page = new PageResponse<>(List.of(), 0, 20, 0, 0, true, true);
        when(projectService.searchProjects("test", ProjectVisibility.PUBLIC, pageable)).thenReturn(page);

        assertEquals(HttpStatus.OK, controller.searchProjects("test", ProjectVisibility.PUBLIC, pageable).getStatusCode());
    }

    @Test @DisplayName("updateProject should update and return")
    void updateProject() {
        ProjectDto dto = ProjectDto.builder().id(1L).name("Updated").build();
        when(projectService.updateProject(eq(1L), any())).thenReturn(dto);

        assertEquals("Updated", controller.updateProject(1L, new CreateProjectRequest()).getBody().getName());
    }

    @Test @DisplayName("deleteProject should return OK message")
    void deleteProject() {
        doNothing().when(projectService).deleteProject(1L);
        ResponseEntity<String> resp = controller.deleteProject(1L);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Project deleted", resp.getBody());
    }

    @Test @DisplayName("archiveProject should archive and return")
    void archiveProject() {
        ProjectDto dto = ProjectDto.builder().id(1L).archived(true).build();
        when(projectService.archiveProject(1L)).thenReturn(dto);

        assertTrue(controller.archiveProject(1L).getBody().getArchived());
    }

    @Test @DisplayName("forkProject should fork and return")
    void forkProject() {
        ProjectDto dto = ProjectDto.builder().id(2L).parentProjectId(1L).build();
        when(projectService.forkProject(1L)).thenReturn(dto);

        assertEquals(2L, controller.forkProject(1L).getBody().getId());
    }

    @Test @DisplayName("star should return OK")
    void star() {
        doNothing().when(projectService).starProject(1L);
        assertEquals(HttpStatus.OK, controller.star(1L).getStatusCode());
    }

    @Test @DisplayName("unstar should return OK")
    void unstar() {
        doNothing().when(projectService).unstarProject(1L);
        assertEquals(HttpStatus.OK, controller.unstar(1L).getStatusCode());
    }

    @Test @DisplayName("getMembers should return member page")
    void getMembers() {
        PageResponse<ProjectMemberDto> page = new PageResponse<>(List.of(), 0, 20, 0, 0, true, true);
        when(projectService.getProjectMembers(1L, pageable)).thenReturn(page);

        assertEquals(HttpStatus.OK, controller.getMembers(1L, pageable).getStatusCode());
    }

    @Test @DisplayName("canEditProject should delegate to service")
    void canEditProject() {
        when(projectService.canEditProject(1L, "user@test.com")).thenReturn(true);
        assertTrue(controller.canEditProject(1L, "user@test.com").getBody());
    }

    @Test @DisplayName("addMember should return 201")
    void addMember() {
        doNothing().when(projectService).addMember(1L, "u@t.com", "EDITOR");
        ResponseEntity<Void> resp = controller.addMember(1L, Map.of("userEmail", "u@t.com", "role", "EDITOR"));
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
    }

    @Test @DisplayName("addMember with default role should use VIEWER")
    void addMember_defaultRole() {
        doNothing().when(projectService).addMember(1L, "u@t.com", "VIEWER");
        Map<String, String> body = new java.util.HashMap<>();
        body.put("userEmail", "u@t.com");
        controller.addMember(1L, body);
        verify(projectService).addMember(1L, "u@t.com", "VIEWER");
    }

    @Test @DisplayName("removeMember should return 204")
    void removeMember() {
        doNothing().when(projectService).removeMember(1L, "u@t.com");
        assertEquals(HttpStatus.NO_CONTENT, controller.removeMember(1L, "u@t.com").getStatusCode());
    }

    @Test @DisplayName("getAllProjectsAdmin should return all projects")
    void getAllProjectsAdmin() {
        PageResponse<ProjectDto> page = new PageResponse<>(List.of(), 0, 20, 0, 0, true, true);
        when(projectService.getAllProjectsAdmin(pageable)).thenReturn(page);

        assertEquals(HttpStatus.OK, controller.getAllProjectsAdmin(pageable).getStatusCode());
    }

    @Test @DisplayName("forceDeleteProject should return OK")
    void forceDeleteProject() {
        doNothing().when(projectService).forceDeleteProject(1L);
        assertEquals("Project deleted by admin", controller.forceDeleteProject(1L).getBody());
    }

    @Test @DisplayName("getAdminProjectStats should return stats map")
    void getAdminProjectStats() {
        when(projectService.getAdminProjectStats()).thenReturn(Map.of("total", 10L));
        assertEquals(10L, controller.getAdminProjectStats().getBody().get("total"));
    }
}
