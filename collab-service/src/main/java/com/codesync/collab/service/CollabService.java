/*
 * Code reader note: Defines collaboration session operations implemented by the service layer.
 */
package com.codesync.collab.service;

import com.codesync.collab.dto.CollabSessionDto;
import com.codesync.collab.entity.Participant;

import java.util.List;
import java.util.Optional;

/**
 * Service defining operations for real-time collaboration sessions.
 * Uses email-based identity (Option A) to match the API Gateway X-User header.
 */
public interface CollabService {

    // Start a new collaboration session on a specific file
    CollabSessionDto createSession(CollabSessionDto sessionDto, String ownerEmail);

    // Get session details by its UUID
    Optional<CollabSessionDto> getSessionById(String sessionId);

    // Get all active sessions for a project
    List<CollabSessionDto> getActiveSessionsByProject(Long projectId);

    // Get all sessions (any status) for a project
    List<CollabSessionDto> getAllSessionsByProject(Long projectId);

    // Join an active session (validates password if protected; enforces max participants)
    Participant joinSession(String sessionId, String userEmail, String password);

    // Leave a session gracefully
    void leaveSession(String sessionId, String userEmail);

    // End an active session (host only)
    void endSession(String sessionId, String ownerEmail);

    // Kick a participant from the session (host only)
    void kickParticipant(String sessionId, String targetEmail, String requesterEmail);

    // Get all currently active participants in a session
    List<Participant> getSessionParticipants(String sessionId);

    // Update cursor position for a participant (called from WebSocket handler)
    void updateCursor(String sessionId, String userEmail, int line, int col);

    // ─── Admin-only operations ────────────────────────────────────────────────

    /** Returns ALL sessions across all projects and statuses (admin only). */
    List<CollabSessionDto> getAllSessionsAdmin();

    /** Force-ends any session regardless of who the owner is (admin only). */
    void forceEndSession(String sessionId);
}
