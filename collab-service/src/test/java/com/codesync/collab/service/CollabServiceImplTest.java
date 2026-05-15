package com.codesync.collab.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.codesync.collab.client.NotificationClient;
import com.codesync.collab.dto.CollabSessionDto;
import com.codesync.collab.entity.CollabSession;
import com.codesync.collab.entity.Participant;
import com.codesync.collab.repository.CollabRepository;
import com.codesync.collab.repository.ParticipantRepository;

@ExtendWith(MockitoExtension.class)
class CollabServiceImplTest {

    @Mock
    private CollabRepository collabRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private CollabServiceImpl collabService;

    private static final String USER = "test@test.com";

    @BeforeEach
    void setUp() {
        var auth = new UsernamePasswordAuthenticationToken(
                USER, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createSession_Success() {
        CollabSessionDto dto = new CollabSessionDto();
        dto.setProjectId(1L);
        dto.setFileId(2L);
        dto.setLanguage("Java");
        dto.setMaxParticipants(5);
        dto.setIsPasswordProtected(false);

        CollabSession session = CollabSession.builder()
                .sessionId("session-id")
                .projectId(1L)
                .status("ACTIVE")
                .build();

        when(collabRepository.save(any(CollabSession.class))).thenReturn(session);
        when(collabRepository.findBySessionId("session-id")).thenReturn(Optional.of(session));
        
        // This is called by joinSession internally
        when(participantRepository.countBySessionIdAndLeftAtIsNull("session-id")).thenReturn(0L);
        when(participantRepository.findBySessionIdAndUserEmailAndLeftAtIsNull(eq("session-id"), anyString())).thenReturn(Optional.empty());
        when(participantRepository.findBySessionId("session-id")).thenReturn(List.of());
        when(participantRepository.save(any(Participant.class))).thenReturn(new Participant());

        CollabSessionDto result = collabService.createSession(dto, USER);

        assertNotNull(result);
        assertEquals("session-id", result.getSessionId());
        verify(collabRepository, times(1)).save(any(CollabSession.class));
    }

    @Test
    void getSessionById_Found() {
        CollabSession session = CollabSession.builder().sessionId("s1").build();
        when(collabRepository.findBySessionId("s1")).thenReturn(Optional.of(session));

        Optional<CollabSessionDto> result = collabService.getSessionById("s1");
        assertTrue(result.isPresent());
        assertEquals("s1", result.get().getSessionId());
    }

    @Test
    void joinSession_Success() {
        CollabSession session = CollabSession.builder()
                .sessionId("s1")
                .status("ACTIVE")
                .maxParticipants(10)
                .build();
        
        when(collabRepository.findBySessionId("s1")).thenReturn(Optional.of(session));
        when(participantRepository.countBySessionIdAndLeftAtIsNull("s1")).thenReturn(1L);
        when(participantRepository.findBySessionIdAndUserEmailAndLeftAtIsNull("s1", "new@t.com")).thenReturn(Optional.empty());
        
        Participant host = Participant.builder().userEmail("host@test.com").role("HOST").build();
        when(participantRepository.findBySessionId("s1")).thenReturn(List.of(host));
        when(participantRepository.save(any(Participant.class))).thenAnswer(i -> i.getArgument(0));

        Participant p = collabService.joinSession("s1", "new@t.com", null);

        assertNotNull(p);
        assertEquals("EDITOR", p.getRole());
        verify(notificationClient).sendSessionJoinNotification(eq("new@t.com"), eq("host@test.com"), eq("s1"));
    }

    @Test
    void leaveSession_Success() {
        Participant p = Participant.builder().sessionId("s1").userEmail(USER).build();
        when(participantRepository.findBySessionIdAndUserEmailAndLeftAtIsNull("s1", USER))
                .thenReturn(Optional.of(p));

        collabService.leaveSession("s1", USER);

        assertNotNull(p.getLeftAt());
        verify(participantRepository).save(p);
    }

    @Test
    void endSession_Success() {
        CollabSession session = CollabSession.builder().sessionId("s1").build();
        when(collabRepository.findBySessionId("s1")).thenReturn(Optional.of(session));

        Participant host = Participant.builder().userEmail(USER).role("HOST").build();
        when(participantRepository.findBySessionId("s1")).thenReturn(List.of(host));

        when(participantRepository.findBySessionIdAndLeftAtIsNull("s1")).thenReturn(List.of(host));

        collabService.endSession("s1", USER);

        assertEquals("ENDED", session.getStatus());
        verify(collabRepository).save(session);
        verify(participantRepository).saveAll(any());
    }

    @Test
    void updateCursor_Success() {
        Participant p = Participant.builder().sessionId("s1").userEmail(USER).build();
        when(participantRepository.findBySessionIdAndUserEmailAndLeftAtIsNull("s1", USER))
                .thenReturn(Optional.of(p));

        collabService.updateCursor("s1", USER, 5, 10);

        assertEquals(5, p.getCursorLine());
        assertEquals(10, p.getCursorCol());
        verify(participantRepository).save(p);
    }

    @Test
    void endIdleSessions_Success() {
        CollabSession session = CollabSession.builder()
                .sessionId("s1")
                .status("ACTIVE")
                .createdAt(java.time.LocalDateTime.now().minusHours(1))
                .build();
        when(collabRepository.findAll()).thenReturn(List.of(session));
        when(participantRepository.countBySessionIdAndLeftAtIsNull("s1")).thenReturn(0L);

        collabService.endIdleSessions();

        assertEquals("ENDED", session.getStatus());
        verify(collabRepository).save(session);
    }

    @Test
    void getActiveSessionsByProject_Success() {
        CollabSession session = CollabSession.builder().sessionId("s1").status("ACTIVE").build();
        when(collabRepository.findByProjectIdAndStatus(1L, "ACTIVE")).thenReturn(List.of(session));

        var list = collabService.getActiveSessionsByProject(1L);
        assertFalse(list.isEmpty());
        assertEquals("s1", list.get(0).getSessionId());
    }

    @Test
    void getAllSessionsByProject_Success() {
        CollabSession session = CollabSession.builder().sessionId("s1").status("ACTIVE").build();
        when(collabRepository.findByProjectId(1L)).thenReturn(List.of(session));

        var list = collabService.getAllSessionsByProject(1L);
        assertFalse(list.isEmpty());
        assertEquals("s1", list.get(0).getSessionId());
    }
}
