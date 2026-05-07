/*
 * Code reader note: Implements session creation, lookup, join/leave/end, participant kick, cursor tracking, idle cleanup, and admin controls.
 */
package com.codesync.collab.service;

import com.codesync.collab.client.NotificationClient;
import com.codesync.collab.dto.CollabSessionDto;
import com.codesync.collab.entity.CollabSession;
import com.codesync.collab.entity.Participant;
import com.codesync.collab.exception.ResourceNotFoundException;
import com.codesync.collab.repository.CollabRepository;
import com.codesync.collab.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of CollabService.
 * Uses email-based identity (Option A) — userEmail matches the X-User gateway header.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollabServiceImpl implements CollabService {

    private final CollabRepository collabRepository;
    private final ParticipantRepository participantRepository;
    private final NotificationClient notificationClient;

    // Palette of distinct cursor colors (cycles by hash of email)
    private static final String[] CURSOR_COLORS = {
        "#FF5733", "#33FF57", "#3357FF", "#F333FF", "#33FFF5",
        "#FFD700", "#FF69B4", "#00CED1", "#FF8C00", "#7B68EE"
    };

    @Override
    @Transactional
    public CollabSessionDto createSession(CollabSessionDto dto, String ownerEmail) {
        // sessionId is a public room identifier for REST/WebSocket clients; the
        // database primary key can stay internal.
        CollabSession session = CollabSession.builder()
                .sessionId(UUID.randomUUID().toString())
                .projectId(dto.getProjectId())
                .fileId(dto.getFileId())
                .ownerId(0L)  // Kept for DB compatibility; ownerEmail is the real identity
                .language(dto.getLanguage())
                // Use a safe default when the client does not choose a room size.
                .maxParticipants(dto.getMaxParticipants() != null ? dto.getMaxParticipants() : 10)
                .isPasswordProtected(Boolean.TRUE.equals(dto.getIsPasswordProtected()))
                .sessionPassword(Boolean.TRUE.equals(dto.getIsPasswordProtected())
                        ? dto.getSessionPassword() : null)
                .build();

        CollabSession saved = collabRepository.save(session);

        // Auto-join the creator as HOST
        joinSession(saved.getSessionId(), ownerEmail, null);

        return mapToDto(saved);
    }

    @Override
    public Optional<CollabSessionDto> getSessionById(String sessionId) {
        return collabRepository.findBySessionId(sessionId).map(this::mapToDto);
    }

    @Override
    public List<CollabSessionDto> getActiveSessionsByProject(Long projectId) {
        return collabRepository.findByProjectIdAndStatus(projectId, "ACTIVE").stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CollabSessionDto> getAllSessionsByProject(Long projectId) {
        return collabRepository.findByProjectId(projectId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Participant joinSession(String sessionId, String userEmail, String password) {
        CollabSession session = collabRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        if ("ENDED".equals(session.getStatus())) {
            throw new IllegalStateException("Session has ended");
        }

        // Max participants check
        // Only participants with leftAt == null count as currently occupying a seat.
        long activeCount = participantRepository.countBySessionIdAndLeftAtIsNull(sessionId);
        if (activeCount >= session.getMaxParticipants()) {
            throw new IllegalStateException("Session is full (" + session.getMaxParticipants() + " participants max)");
        }

        // Password validation
        if (Boolean.TRUE.equals(session.getIsPasswordProtected())) {
            if (password == null || !password.equals(session.getSessionPassword())) {
                throw new SecurityException("Incorrect session password");
            }
        }

        // If already an active participant, just return existing record
        Optional<Participant> existing = participantRepository
                .findBySessionIdAndUserEmailAndLeftAtIsNull(sessionId, userEmail);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Assign deterministic cursor color from email hash
        // The same email gets the same color every time it joins this service.
        String color = CURSOR_COLORS[Math.abs(userEmail.hashCode()) % CURSOR_COLORS.length];

        // Determine role: first participant is HOST, others are EDITOR
        // The creator auto-joins immediately after session creation, making them
        // the first participant and therefore the host.
        boolean isHost = participantRepository.findBySessionId(sessionId).isEmpty();

        Participant participant = Participant.builder()
                .sessionId(sessionId)
                .userEmail(userEmail)
                .role(isHost ? "HOST" : "EDITOR")
                .color(color)
                .build();

        Participant saved = participantRepository.save(participant);

        // Notify the joining user (skip self-notification for the host who created the session)
        if (!isHost) {
            // Find host email to set as actorEmail
            participantRepository.findBySessionId(sessionId).stream()
                    .filter(p -> "HOST".equals(p.getRole()))
                    .findFirst()
                    .ifPresent(host -> notificationClient.sendSessionJoinNotification(
                            userEmail, host.getUserEmail(), sessionId));
        }

        return saved;
    }

    @Override
    @Transactional
    public void leaveSession(String sessionId, String userEmail) {
        Participant participant = participantRepository
                .findBySessionIdAndUserEmailAndLeftAtIsNull(sessionId, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Active participant not found"));

        participant.setLeftAt(LocalDateTime.now());
        participantRepository.save(participant);
        log.info("User {} left session {}", userEmail, sessionId);
    }

    @Override
    @Transactional
    public void endSession(String sessionId, String ownerEmail) {
        CollabSession session = collabRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        // Verify the requester is the host
        // Host status is stored on the Participant row, not the session row, so
        // the check looks through session participants.
        boolean isHost = participantRepository.findBySessionId(sessionId).stream()
                .anyMatch(p -> ownerEmail.equals(p.getUserEmail()) && "HOST".equals(p.getRole()));
        if (!isHost) {
            throw new SecurityException("Only the session host can end the session");
        }

        session.setStatus("ENDED");
        session.setEndedAt(LocalDateTime.now());
        collabRepository.save(session);

        // Mark all remaining participants as left
        // This closes the session cleanly for participant queries that only look
        // at rows where leftAt is still null.
        List<Participant> activeParticipants = participantRepository.findBySessionIdAndLeftAtIsNull(sessionId);
        LocalDateTime now = LocalDateTime.now();
        activeParticipants.forEach(p -> p.setLeftAt(now));
        participantRepository.saveAll(activeParticipants);
        log.info("Session {} ended by {}", sessionId, ownerEmail);
    }

    @Override
    @Transactional
    public void kickParticipant(String sessionId, String targetEmail, String requesterEmail) {
        // Only host can kick
        boolean isHost = participantRepository.findBySessionId(sessionId).stream()
                .anyMatch(p -> requesterEmail.equals(p.getUserEmail()) && "HOST".equals(p.getRole()));
        if (!isHost) {
            throw new SecurityException("Only the session host can kick participants");
        }

        Participant target = participantRepository
                .findBySessionIdAndUserEmailAndLeftAtIsNull(sessionId, targetEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Target participant not found: " + targetEmail));

        target.setLeftAt(LocalDateTime.now());
        participantRepository.save(target);

        // Notify the kicked user
        notificationClient.sendSessionKickNotification(targetEmail, requesterEmail, sessionId);

        log.info("User {} kicked {} from session {}", requesterEmail, targetEmail, sessionId);
    }

    @Override
    public List<Participant> getSessionParticipants(String sessionId) {
        return participantRepository.findBySessionIdAndLeftAtIsNull(sessionId);
    }

    @Override
    @Transactional
    public void updateCursor(String sessionId, String userEmail, int line, int col) {
        participantRepository.findBySessionIdAndUserEmailAndLeftAtIsNull(sessionId, userEmail)
                .ifPresent(p -> {
                    p.setCursorLine(line);
                    p.setCursorCol(col);
                    participantRepository.save(p);
                });
    }

    // ─── Internal helpers ────────────────────────────────────────────────────

    private CollabSessionDto mapToDto(CollabSession session) {
        long count = participantRepository.countBySessionIdAndLeftAtIsNull(session.getSessionId());
        return CollabSessionDto.builder()
                .sessionId(session.getSessionId())
                .projectId(session.getProjectId())
                .fileId(session.getFileId())
                .status(session.getStatus())
                .language(session.getLanguage())
                .maxParticipants(session.getMaxParticipants())
                .isPasswordProtected(session.getIsPasswordProtected())
                .participantCount((int) count)
                .createdAt(session.getCreatedAt())
                .endedAt(session.getEndedAt())
                .build();
    }

    /**
     * Called by the scheduled cleanup task — ends sessions with no activity for 30+ minutes.
     */
    @Transactional
    public void endIdleSessions() {
        // A session is considered idle only when it is active, empty, and older
        // than the cutoff. Active participants prevent automatic cleanup.
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        collabRepository.findAll().stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .filter(s -> {
                    long activeCount = participantRepository.countBySessionIdAndLeftAtIsNull(s.getSessionId());
                    return activeCount == 0 && s.getCreatedAt().isBefore(cutoff);
                })
                .forEach(s -> {
                    s.setStatus("ENDED");
                    s.setEndedAt(LocalDateTime.now());
                    collabRepository.save(s);
                    log.info("Auto-ended idle session {}", s.getSessionId());
                });
    }

    // ─── Admin-only operations ────────────────────────────────────────────────

    @Override
    public List<CollabSessionDto> getAllSessionsAdmin() {
        return collabRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void forceEndSession(String sessionId) {
        CollabSession session = collabRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        session.setStatus("ENDED");
        session.setEndedAt(LocalDateTime.now());
        collabRepository.save(session);

        List<Participant> active = participantRepository.findBySessionIdAndLeftAtIsNull(sessionId);
        LocalDateTime now = LocalDateTime.now();
        active.forEach(p -> p.setLeftAt(now));
        participantRepository.saveAll(active);
        log.info("Session {} force-ended by admin", sessionId);
    }
}
