package com.codesync.collab.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.codesync.collab.dto.CollabSessionDto;
import com.codesync.collab.entity.Participant;
import com.codesync.collab.service.CollabService;

/** Unit tests for {@link CollabController}. */
@ExtendWith(MockitoExtension.class)
class CollabControllerTest {

    @Mock private CollabService collabService;
    @InjectMocks private CollabController controller;
    private static final String USER = "user@test.com";

    @Test @DisplayName("createSession returns 201")
    void createSession() {
        CollabSessionDto dto = new CollabSessionDto();
        when(collabService.createSession(any(), eq(USER))).thenReturn(dto);
        assertEquals(HttpStatus.CREATED, controller.createSession(dto, USER).getStatusCode());
    }

    @Test @DisplayName("getSession found returns 200")
    void getSession_found() {
        CollabSessionDto dto = new CollabSessionDto();
        when(collabService.getSessionById("s1")).thenReturn(Optional.of(dto));
        assertEquals(HttpStatus.OK, controller.getSession("s1").getStatusCode());
    }

    @Test @DisplayName("getSession not found returns 404")
    void getSession_notFound() {
        when(collabService.getSessionById("s1")).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, controller.getSession("s1").getStatusCode());
    }

    @Test @DisplayName("getActiveSessions returns list")
    void getActiveSessions() {
        when(collabService.getActiveSessionsByProject(1L)).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.getActiveSessions(1L).getStatusCode());
    }

    @Test @DisplayName("getAllSessions returns list")
    void getAllSessions() {
        when(collabService.getAllSessionsByProject(1L)).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.getAllSessions(1L).getStatusCode());
    }

    @Test @DisplayName("joinSession with password returns participant")
    void joinSession_withPassword() {
        Participant p = new Participant();
        when(collabService.joinSession("s1", USER, "pass")).thenReturn(p);
        assertEquals(HttpStatus.OK, controller.joinSession("s1", Map.of("password", "pass"), USER).getStatusCode());
    }

    @Test @DisplayName("joinSession without body returns participant")
    void joinSession_noBody() {
        Participant p = new Participant();
        when(collabService.joinSession("s1", USER, null)).thenReturn(p);
        assertEquals(HttpStatus.OK, controller.joinSession("s1", null, USER).getStatusCode());
    }

    @Test @DisplayName("leaveSession returns 200")
    void leaveSession() {
        doNothing().when(collabService).leaveSession("s1", USER);
        assertEquals(HttpStatus.OK, controller.leaveSession("s1", USER).getStatusCode());
    }

    @Test @DisplayName("endSession returns 200")
    void endSession() {
        doNothing().when(collabService).endSession("s1", USER);
        assertEquals(HttpStatus.OK, controller.endSession("s1", USER).getStatusCode());
    }

    @Test @DisplayName("kickParticipant returns 200")
    void kickParticipant() {
        doNothing().when(collabService).kickParticipant("s1", "target@t.com", USER);
        assertEquals(HttpStatus.OK, controller.kickParticipant("s1", "target@t.com", USER).getStatusCode());
    }

    @Test @DisplayName("getParticipants returns list")
    void getParticipants() {
        when(collabService.getSessionParticipants("s1")).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.getParticipants("s1").getStatusCode());
    }

    @Test @DisplayName("getAllSessionsAdmin returns all")
    void getAllSessionsAdmin() {
        when(collabService.getAllSessionsAdmin()).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.getAllSessionsAdmin().getStatusCode());
    }

    @Test @DisplayName("forceEndSession returns OK")
    void forceEndSession() {
        doNothing().when(collabService).forceEndSession("s1");
        assertEquals("Session force-ended by admin", controller.forceEndSession("s1").getBody());
    }
}
