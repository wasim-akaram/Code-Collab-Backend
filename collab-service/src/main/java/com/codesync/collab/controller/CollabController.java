/*
 * Code reader note: Exposes REST endpoints for collaboration session lifecycle,
 * participants, owner actions, and admin session controls.
 * Annotations used: @RestController publishes the API, @RequestMapping sets the
 * /sessions base path, @RequiredArgsConstructor injects the service, and the
 * mapping annotations define each HTTP route. @PreAuthorize protects admin actions.
 */
package com.codesync.collab.controller;

import com.codesync.collab.dto.CollabSessionDto;
import com.codesync.collab.entity.Participant;
import com.codesync.collab.service.CollabService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for managing collaboration sessions.
 * X-User header contains the user's email (set by the API Gateway).
 */
@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class CollabController {

    private final CollabService collabService;

    /** Create a new collaboration session. */
    @PostMapping
    public ResponseEntity<CollabSessionDto> createSession(@RequestBody CollabSessionDto dto,
                                                          @RequestHeader("X-User") String userEmail) {
        return new ResponseEntity<>(collabService.createSession(dto, userEmail), HttpStatus.CREATED);
    }

    /** Get a specific session by its UUID. */
    @GetMapping("/{sessionId}")
    public ResponseEntity<CollabSessionDto> getSession(@PathVariable String sessionId) {
        return collabService.getSessionById(sessionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Get all active sessions for a project. */
    @GetMapping("/project/{projectId}/active")
    public ResponseEntity<List<CollabSessionDto>> getActiveSessions(@PathVariable Long projectId) {
        return ResponseEntity.ok(collabService.getActiveSessionsByProject(projectId));
    }

    /** Get all sessions (any status) for a project. */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<CollabSessionDto>> getAllSessions(@PathVariable Long projectId) {
        return ResponseEntity.ok(collabService.getAllSessionsByProject(projectId));
    }

    /** Join a session (validates password if protected). */
    @PostMapping("/{sessionId}/join")
    public ResponseEntity<Participant> joinSession(@PathVariable String sessionId,
                                                   @RequestBody(required = false) Map<String, String> body,
                                                   @RequestHeader("X-User") String userEmail) {
        String password = body != null ? body.get("password") : null;
        return ResponseEntity.ok(collabService.joinSession(sessionId, userEmail, password));
    }

    /** Leave a session gracefully. */
    @PostMapping("/{sessionId}/leave")
    public ResponseEntity<Void> leaveSession(@PathVariable String sessionId,
                                             @RequestHeader("X-User") String userEmail) {
        collabService.leaveSession(sessionId, userEmail);
        return ResponseEntity.ok().build();
    }

    /** End a session (host only). */
    @PostMapping("/{sessionId}/end")
    public ResponseEntity<Void> endSession(@PathVariable String sessionId,
                                           @RequestHeader("X-User") String userEmail) {
        collabService.endSession(sessionId, userEmail);
        return ResponseEntity.ok().build();
    }

    /** Kick a participant from the session (host only). */
    @PostMapping("/{sessionId}/kick/{targetEmail}")
    public ResponseEntity<Void> kickParticipant(@PathVariable String sessionId,
                                                @PathVariable String targetEmail,
                                                @RequestHeader("X-User") String userEmail) {
        collabService.kickParticipant(sessionId, targetEmail, userEmail);
        return ResponseEntity.ok().build();
    }

    /** Get all currently active participants in a session. */
    @GetMapping("/{sessionId}/participants")
    public ResponseEntity<List<Participant>> getParticipants(@PathVariable String sessionId) {
        return ResponseEntity.ok(collabService.getSessionParticipants(sessionId));
    }

    // ─── Admin-only endpoints ─────────────────────────────────────────────────

    /** Get ALL sessions across all projects (admin only). */
    @GetMapping("/admin/all")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<com.codesync.collab.dto.CollabSessionDto>> getAllSessionsAdmin() {
        return ResponseEntity.ok(collabService.getAllSessionsAdmin());
    }

    /** Force-end any session regardless of ownership (admin only). */
    @PostMapping("/admin/{sessionId}/force-end")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> forceEndSession(@PathVariable String sessionId) {
        collabService.forceEndSession(sessionId);
        return ResponseEntity.ok("Session force-ended by admin");
    }
}
