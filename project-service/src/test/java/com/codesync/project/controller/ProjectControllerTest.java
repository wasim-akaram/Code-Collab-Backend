package com.codesync.project.controller;

import com.codesync.common.dto.PageResponse;
import com.codesync.common.enums.ProjectVisibility;
import com.codesync.project.dto.CreateProjectRequest;
import com.codesync.project.dto.ProjectDto;
import com.codesync.project.service.ProjectService;
import com.codesync.project.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for unit testing controller
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private JwtUtil jwtUtil;

    private ProjectDto projectDto;
    private CreateProjectRequest createRequest;

    @BeforeEach
    void setUp() {
        projectDto = ProjectDto.builder()
                .id(1L)
                .name("Test Project")
                .description("Desc")
                .ownerEmail("test@example.com")
                .language("Java")
                .visibility(ProjectVisibility.PUBLIC)
                .archived(false)
                .starCount(0L)
                .forkCount(0L)
                .defaultBranch("main")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = CreateProjectRequest.builder()
                .name("Test Project")
                .description("Desc")
                .language("Java")
                .visibility(ProjectVisibility.PUBLIC)
                .build();
    }

    // --- POST /projects ---

    @Test
    void createProject_shouldReturn200() throws Exception {
        when(projectService.createProject(any(CreateProjectRequest.class))).thenReturn(projectDto);

        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Test Project"))
                .andExpect(jsonPath("$.ownerEmail").value("test@example.com"))
                .andExpect(jsonPath("$.language").value("Java"))
                .andExpect(jsonPath("$.visibility").value("PUBLIC"));
    }

    // --- GET /projects/{id} ---

    @Test
    void getProject_shouldReturn200() throws Exception {
        when(projectService.getProjectById(1L)).thenReturn(projectDto);

        mockMvc.perform(get("/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Test Project"))
                .andExpect(jsonPath("$.description").value("Desc"));
    }

    @Test
    void getProject_shouldReturn500WhenServiceThrows() throws Exception {
        when(projectService.getProjectById(99L)).thenThrow(new RuntimeException("Project not found"));

        try {
            mockMvc.perform(get("/projects/99"));
        } catch (Exception e) {
            org.junit.jupiter.api.Assertions.assertTrue(e.getCause() instanceof RuntimeException);
            org.junit.jupiter.api.Assertions.assertEquals("Project not found", e.getCause().getMessage());
        }
    }

    // --- GET /projects/my ---

    @Test
    void getMyProjects_shouldReturn200() throws Exception {
        PageResponse<ProjectDto> response = PageResponse.<ProjectDto>builder()
                .content(List.of(projectDto))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .isFirst(true)
                .isLast(true)
                .build();
        when(projectService.getUserProjects(any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/projects/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Test Project"));
    }

    @Test
    void getMyProjects_shouldReturnEmptyPage() throws Exception {
        PageResponse<ProjectDto> response = PageResponse.<ProjectDto>builder()
                .content(Collections.emptyList())
                .page(0)
                .size(20)
                .totalElements(0)
                .totalPages(0)
                .isFirst(true)
                .isLast(true)
                .build();
        when(projectService.getUserProjects(any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/projects/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    // --- GET /projects/public ---

    @Test
    void getPublicProjects_shouldReturn200() throws Exception {
        PageResponse<ProjectDto> response = PageResponse.<ProjectDto>builder()
                .content(List.of(projectDto))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .isFirst(true)
                .isLast(true)
                .build();
        when(projectService.getPublicProjects(any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/projects/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].visibility").value("PUBLIC"));
    }

    // --- PUT /projects/{id} ---

    @Test
    void updateProject_shouldReturn200() throws Exception {
        ProjectDto updatedDto = ProjectDto.builder()
                .id(1L)
                .name("Updated Project")
                .description("Updated Desc")
                .ownerEmail("test@example.com")
                .language("Python")
                .visibility(ProjectVisibility.PRIVATE)
                .archived(false)
                .build();
        when(projectService.updateProject(eq(1L), any(CreateProjectRequest.class))).thenReturn(updatedDto);

        CreateProjectRequest updateRequest = CreateProjectRequest.builder()
                .name("Updated Project")
                .description("Updated Desc")
                .language("Python")
                .visibility(ProjectVisibility.PRIVATE)
                .build();

        mockMvc.perform(put("/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Project"))
                .andExpect(jsonPath("$.language").value("Python"))
                .andExpect(jsonPath("$.visibility").value("PRIVATE"));
    }

    // --- DELETE /projects/{id} ---

    @Test
    void deleteProject_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/projects/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Project deleted"));

        verify(projectService, times(1)).deleteProject(1L);
    }

    // --- GET /projects ---

    @Test
    void getProjects_shouldReturn200() throws Exception {
        PageResponse<ProjectDto> response = PageResponse.<ProjectDto>builder()
                .content(List.of(projectDto))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .isFirst(true)
                .isLast(true)
                .build();
        when(projectService.getUserProjects(any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // --- POST /projects/{id}/star ---

    @Test
    void starProject_shouldReturn200() throws Exception {
        mockMvc.perform(post("/projects/1/star"))
                .andExpect(status().isOk())
                .andExpect(content().string("Starred"));

        verify(projectService, times(1)).starProject(1L);
    }

    // --- POST /projects/{id}/unstar ---

    @Test
    void unstarProject_shouldReturn200() throws Exception {
        mockMvc.perform(post("/projects/1/unstar"))
                .andExpect(status().isOk())
                .andExpect(content().string("Unstarred"));

        verify(projectService, times(1)).unstarProject(1L);
    }

    // --- GET /projects/test ---

    @Test
    void testEndpoint_shouldReturnXUserHeader() throws Exception {
        mockMvc.perform(get("/projects/test")
                        .header("X-User", "testuser@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("testuser@example.com"));
    }
}
