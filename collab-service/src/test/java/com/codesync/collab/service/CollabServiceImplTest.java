package com.codesync.collab.service;

import com.codesync.collab.client.NotificationClient;
import com.codesync.collab.dto.CollabSessionDto;
import com.codesync.collab.entity.CollabSession;
import com.codesync.collab.entity.Participant;
import com.codesync.collab.exception.ResourceNotFoundException;
import com.codesync.collab.repository.CollabRepository;
import com.codesync.collab.repository.ParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollabServiceImplTest {

    @Mock private CollabRepository collabRepository;
    @Mock private ParticipantRepository participantRepository;
    @Mock private NotificationClient notificationClient;

    @InjectMocks
    private CollabServiceImpl collabService;

    private CollabSession session;
    private Participant hostParticipant;
    private Participant editorParticipant;

    private static final String HOST_EMAIL  = "host@example.com";
    private static final String EDITOR_EMAIL = "editor@example.com";
    private static final String SESSION_ID  = "sess-1";

    @BeforeEach
    void setUp() {
        session = CollabSession.builder()
                .sessionId(SESSION_ID)
                .projectId(10L)
                .ownerId(0L)
                .status("ACTIVE")
                .maxParticipants(10)
                .isPasswordProtected(false)
                .build();

        hostParticipant = Participant.builder()
                .participantId(1L)
                .sessionId(SESSION_ID)
                .userEmail(HOST_EMAIL)
                .role("HOST")
                .build();

        editorParticipant = Participant.builder()
                .participantId(2L)
                .sessionId(SESSION_ID)
                .userEmail(EDITOR_EMAIL)
                .role("EDITOR")
                .build();
    }

    // ─── createSession ──────────────────────────────────────────────────────────

    @Test
    void createSession_shouldSaveSessionAndAutoJoinOwner() {
        CollabSessionDto dto = CollabSessionDto.builder().projectId(10L).build();

        when(collabRepository.save(any(CollabSession.class))).thenReturn(session);
        // Auto-join calls joinSession which needs these stubs
        when(collabRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));
        when(participantRepository.countBySessionIdAndLeftAtIsNull(SESSION_ID)).thenReturn(0L);
        when(participantRepository.findBySessionIdAndUserEmailAndLeftAtIsNull(SESSION_ID, HOST_EMAIL))
                .thenReturn(Optional.empty());
        when(participantRepository.findBySessionId(SESSION_ID)).thenReturn(List.of()); // no prior participants → HOST
        when(participantRepository.save(any(Participant.class))).thenReturn(hostParticipant);

        CollabSessionDto result = collabService.createSession(dto, HOST_EMAIL);

        assertNotNull(result);
        assertEquals(10L, result.getProjectId());
        verify(collabRepository).save(any(CollabSession.class));
    }

    // ─── getSessionById ─────────────────────────────────────────────────────────

    @Test
    void getSessionById_shouldReturnMappedDto() {
        when(collabRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));
        when(participantRepository.countBySessionIdAndLeftAtIsNull(SESSION_ID)).thenReturn(1L);

        Optional<CollabSessionDto> result = collabService.getSessionById(SESSION_ID);

        assertTrue(result.isPresent());
        assertEquals(SESSION_ID, result.get().getSessionId());
    }

    @Test
    void getSessionById_shouldReturnEmptyWhenNotFound() {
        when(collabRepository.findBySessionId("missing")).thenReturn(Optional.empty());

        Optional<CollabSessionDto> result = collabService.getSessionById("missing");
        assertFalse(result.isPresent());
    }

    // ─── joinSession ────────────────────────────────────────────────────────────

    @Test
    void joinSession_shouldJoinAsEditor() {
        when(collabRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));
        when(participantRepository.countBySessionIdAndLeftAtIsNull(SESSION_ID)).thenReturn(1L); // 1 < 10
        when(participantRepository.findBySessionIdAndUserEmailAndLeftAtIsNull(SESSION_ID, EDITOR_EMAIL))
                .thenReturn(Optional.empty()); // not yet joined
        when(participantRepository.findBySessionId(SESSION_ID))
                .thenReturn(List.of(hostParticipant)); // there is already a HOST
        when(participantRepository.save(any(Participant.class))).thenReturn(editorParticipant);

        Participant result = collabService.joinSession(SESSION_ID, EDITOR_EMAIL, null);

        assertNotNull(result);
        verify(participantRepository).save(any(Participant.class));
    }

    @Test
    void joinSession_shouldReturnExistingParticipantIfAlreadyJoined() {
        when(collabRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));
        when(participantRepository.countBySessionIdAndLeftAtIsNull(SESSION_ID)).thenReturn(1L);
        when(participantRepository.findBySessionIdAndUserEmailAndLeftAtIsNull(SESSION_ID, HOST_EMAIL))
                .thenReturn(Optional.of(hostParticipant)); // already joined

        Participant result = collabService.joinSession(SESSION_ID, HOST_EMAIL, null);

        assertEquals(hostParticipant, result);
        verify(participantRepository, never()).save(any());
    }

    @Test
    void joinSession_shouldThrowWhenSessionEnded() {
        session.setStatus("ENDED");
        when(collabRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));

        assertThrows(IllegalStateException.class,
                () -> collabService.joinSession(SESSION_ID, EDITOR_EMAIL, null));
    }

    @Test
    void joinSession_shouldThrowWhenSessionFull() {
        session.setMaxParticipants(2);
        when(collabRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));
        when(participantRepository.countBySessionIdAndLeftAtIsNull(SESSION_ID)).thenReturn(2L); // at cap

        assertThrows(IllegalStateException.class,
                () -> collabService.joinSession(SESSION_ID, EDITOR_EMAIL, null));
    }

    @Test
    void joinSession_shouldThrowWhenWrongPassword() {
        session.setIsPasswordProtected(true);
        session.setSessionPassword("secret");
        when(collabRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));
        when(participantRepository.countBySessionIdAndLeftAtIsNull(SESSION_ID)).thenReturn(0L);

        assertThrows(SecurityException.class,
                () -> collabService.joinSession(SESSION_ID, EDITOR_EMAIL, "wrong"));
    }

    @Test
    void joinSession_shouldThrowWhenSessionNotFound() {
        when(collabRepository.findBySessionId("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> collabService.joinSession("missing", EDITOR_EMAIL, null));
    }

    // ─── leaveSession ───────────────────────────────────────────────────────────

    @Test
    void leaveSession_shouldSetLeftAt() {
        when(participantRepository.findBySessionIdAndUserEmailAndLeftAtIsNull(SESSION_ID, HOST_EMAIL))
                .thenReturn(Optional.of(hostParticipant));

        collabService.leaveSession(SESSION_ID, HOST_EMAIL);

        assertNotNull(hostParticipant.getLeftAt());
        verify(participantRepository).save(hostParticipant);
    }

    @Test
    void leaveSession_shouldThrowWhenParticipantNotFound() {
        when(participantRepository.findBySessionIdAndUserEmailAndLeftAtIsNull(SESSION_ID, "nobody@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> collabService.leaveSession(SESSION_ID, "nobody@example.com"));
    }

    // ─── endSession ─────────────────────────────────────────────────────────────

    @Test
    void endSession_shouldEndSessionAndMarkParticipantsLeft() {
        when(collabRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));
        // Host check
        when(participantRepository.findBySessionId(SESSION_ID))
                .thenReturn(List.of(hostParticipant, editorParticipant));
        // Active participants to mark as left
        when(participantRepository.findBySessionIdAndLeftAtIsNull(SESSION_ID))
                .thenReturn(List.of(hostParticipant, editorParticipant));

        collabService.endSession(SESSION_ID, HOST_EMAIL);

        assertEquals("ENDED", session.getStatus());
        assertNotNull(session.getEndedAt());
        assertNotNull(hostParticipant.getLeftAt());
        assertNotNull(editorParticipant.getLeftAt());
        verify(collabRepository).save(session);
        verify(participantRepository).saveAll(any());
    }

    @Test
    void endSession_shouldThrowWhenNonHostTriesToEnd() {
        when(collabRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));
        when(participantRepository.findBySessionId(SESSION_ID))
                .thenReturn(List.of(hostParticipant)); // only hostParticipant is HOST

        assertThrows(SecurityException.class,
                () -> collabService.endSession(SESSION_ID, EDITOR_EMAIL));
    }

    // ─── kickParticipant ────────────────────────────────────────────────────────

    @Test
    void kickParticipant_shouldSetLeftAtOnTarget() {
        when(participantRepository.findBySessionId(SESSION_ID))
                .thenReturn(List.of(hostParticipant)); // requester is HOST
        when(participantRepository.findBySessionIdAndUserEmailAndLeftAtIsNull(SESSION_ID, EDITOR_EMAIL))
                .thenReturn(Optional.of(editorParticipant));
        doNothing().when(notificationClient).sendSessionKickNotification(any(), any(), any());

        collabService.kickParticipant(SESSION_ID, EDITOR_EMAIL, HOST_EMAIL);

        assertNotNull(editorParticipant.getLeftAt());
        verify(participantRepository).save(editorParticipant);
        verify(notificationClient).sendSessionKickNotification(EDITOR_EMAIL, HOST_EMAIL, SESSION_ID);
    }

    @Test
    void kickParticipant_shouldThrowWhenNonHostKicks() {
        when(participantRepository.findBySessionId(SESSION_ID))
                .thenReturn(List.of(hostParticipant)); // HOST is hostParticipant, not editorParticipant

        assertThrows(SecurityException.class,
                () -> collabService.kickParticipant(SESSION_ID, HOST_EMAIL, EDITOR_EMAIL));
    }

    @Test
    void kickParticipant_shouldThrowWhenTargetNotInSession() {
        when(participantRepository.findBySessionId(SESSION_ID))
                .thenReturn(List.of(hostParticipant));
        when(participantRepository.findBySessionIdAndUserEmailAndLeftAtIsNull(SESSION_ID, "nobody@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> collabService.kickParticipant(SESSION_ID, "nobody@example.com", HOST_EMAIL));
    }

    // ─── getActiveSessionsByProject ─────────────────────────────────────────────

    @Test
    void getActiveSessionsByProject_shouldReturnActiveSessions() {
        when(collabRepository.findByProjectIdAndStatus(10L, "ACTIVE"))
                .thenReturn(List.of(session));
        when(participantRepository.countBySessionIdAndLeftAtIsNull(SESSION_ID)).thenReturn(1L);

        List<CollabSessionDto> result = collabService.getActiveSessionsByProject(10L);

        assertEquals(1, result.size());
        assertEquals(SESSION_ID, result.get(0).getSessionId());
    }

    @Test
    void getActiveSessionsByProject_shouldReturnEmptyWhenNone() {
        when(collabRepository.findByProjectIdAndStatus(10L, "ACTIVE")).thenReturn(List.of());

        assertTrue(collabService.getActiveSessionsByProject(10L).isEmpty());
    }

    // ─── getAllSessionsByProject ─────────────────────────────────────────────────

    @Test
    void getAllSessionsByProject_shouldReturnAllSessions() {
        CollabSession ended = CollabSession.builder().sessionId("sess-2").projectId(10L)
                .status("ENDED").ownerId(0L).maxParticipants(10).build();
        when(collabRepository.findByProjectId(10L)).thenReturn(List.of(session, ended));
        when(participantRepository.countBySessionIdAndLeftAtIsNull(any())).thenReturn(0L);

        List<CollabSessionDto> result = collabService.getAllSessionsByProject(10L);

        assertEquals(2, result.size());
    }

    // ─── getSessionParticipants ─────────────────────────────────────────────────

    @Test
    void getSessionParticipants_shouldReturnActiveOnes() {
        when(participantRepository.findBySessionIdAndLeftAtIsNull(SESSION_ID))
                .thenReturn(List.of(hostParticipant, editorParticipant));

        List<Participant> result = collabService.getSessionParticipants(SESSION_ID);

        assertEquals(2, result.size());
    }

    // ─── updateCursor ───────────────────────────────────────────────────────────

    @Test
    void updateCursor_shouldUpdateCursorPosition() {
        when(participantRepository.findBySessionIdAndUserEmailAndLeftAtIsNull(SESSION_ID, HOST_EMAIL))
                .thenReturn(Optional.of(hostParticipant));

        collabService.updateCursor(SESSION_ID, HOST_EMAIL, 15, 8);

        assertEquals(15, hostParticipant.getCursorLine());
        assertEquals(8, hostParticipant.getCursorCol());
        verify(participantRepository).save(hostParticipant);
    }

    @Test
    void updateCursor_shouldSilentlyIgnoreWhenParticipantNotFound() {
        when(participantRepository.findBySessionIdAndUserEmailAndLeftAtIsNull(SESSION_ID, "ghost@example.com"))
                .thenReturn(Optional.empty());

        // Should not throw
        assertDoesNotThrow(() -> collabService.updateCursor(SESSION_ID, "ghost@example.com", 1, 1));
        verify(participantRepository, never()).save(any());
    }

    // ─── forceEndSession (admin) ─────────────────────────────────────────────────

    @Test
    void forceEndSession_shouldEndSessionAndMarkAllLeft() {
        when(collabRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(session));
        when(participantRepository.findBySessionIdAndLeftAtIsNull(SESSION_ID))
                .thenReturn(List.of(hostParticipant, editorParticipant));

        collabService.forceEndSession(SESSION_ID);

        assertEquals("ENDED", session.getStatus());
        assertNotNull(hostParticipant.getLeftAt());
        verify(collabRepository).save(session);
        verify(participantRepository).saveAll(any());
    }

    @Test
    void forceEndSession_shouldThrowWhenSessionNotFound() {
        when(collabRepository.findBySessionId("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> collabService.forceEndSession("missing"));
    }
}
