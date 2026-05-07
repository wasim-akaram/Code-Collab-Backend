package com.codesync.collab.controller;

import com.codesync.collab.dto.CollabSessionDto;
import com.codesync.collab.entity.Participant;
import com.codesync.collab.service.CollabService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Fixed CollabControllerTest — all methods use userEmail (String) not userId (Long),
 * matching the current real service implementation.
 */
@WebMvcTest(controllers = CollabController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class CollabControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private CollabService collabService;
    @Autowired private ObjectMapper objectMapper;

    private static final String HOST_EMAIL   = "host@example.com";
    private static final String EDITOR_EMAIL = "editor@example.com";

    private CollabSessionDto sessionDto;
    private Participant participant;

    @BeforeEach
    void setUp() {
        sessionDto = CollabSessionDto.builder()
                .sessionId("session-123")
                .projectId(10L)
                .fileId(20L)
                .status("ACTIVE")
                .build();

        // Participant now uses userEmail (String), not userId (Long)
        participant = Participant.builder()
                .sessionId("session-123")
                .userEmail(HOST_EMAIL)
                .role("HOST")
                .color("#FF5733")
                .build();
    }

    @Test
    void createSession() throws Exception {
        when(collabService.createSession(any(CollabSessionDto.class), eq(HOST_EMAIL))).thenReturn(sessionDto);

        mockMvc.perform(post("/sessions")
                .header("X-User", HOST_EMAIL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sessionDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value("session-123"))
                .andExpect(jsonPath("$.projectId").value(10L));

        verify(collabService).createSession(any(CollabSessionDto.class), eq(HOST_EMAIL));
    }

    @Test
    void getActiveSessions() throws Exception {
        when(collabService.getActiveSessionsByProject(10L)).thenReturn(Arrays.asList(sessionDto));

        mockMvc.perform(get("/sessions/project/10/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sessionId").value("session-123"));

        verify(collabService).getActiveSessionsByProject(10L);
    }

    @Test
    void joinSession() throws Exception {
        when(collabService.joinSession(eq("session-123"), eq(EDITOR_EMAIL), eq("secret"))).thenReturn(participant);

        Map<String, String> body = new HashMap<>();
        body.put("password", "secret");

        mockMvc.perform(post("/sessions/session-123/join")
                .header("X-User", EDITOR_EMAIL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("HOST"));

        verify(collabService).joinSession(eq("session-123"), eq(EDITOR_EMAIL), eq("secret"));
    }

    @Test
    void leaveSession() throws Exception {
        mockMvc.perform(post("/sessions/session-123/leave")
                .header("X-User", EDITOR_EMAIL))
                .andExpect(status().isOk());

        verify(collabService).leaveSession("session-123", EDITOR_EMAIL);
    }

    @Test
    void endSession() throws Exception {
        mockMvc.perform(post("/sessions/session-123/end")
                .header("X-User", HOST_EMAIL))
                .andExpect(status().isOk());

        verify(collabService).endSession("session-123", HOST_EMAIL);
    }

    @Test
    void getParticipants() throws Exception {
        when(collabService.getSessionParticipants("session-123")).thenReturn(Arrays.asList(participant));

        mockMvc.perform(get("/sessions/session-123/participants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userEmail").value(HOST_EMAIL));

        verify(collabService).getSessionParticipants("session-123");
    }
}
