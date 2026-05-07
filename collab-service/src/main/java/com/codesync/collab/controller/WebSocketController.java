/*
 * Code reader note: Handles STOMP WebSocket messages for cursor updates, edits, joins, and leaves during live collaboration.
 */
package com.codesync.collab.controller;

import com.codesync.collab.service.CollabService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * WebSocket STOMP message handler for real-time collaboration events.
 *
 * Clients connect to: ws://localhost:8080/ws-collab (via SockJS)
 * Send to:   /app/session/{id}/cursor  — cursor position updates
 *            /app/session/{id}/edit    — code delta events
 *            /app/session/{id}/join    — announce join
 *            /app/session/{id}/leave   — announce leave
 *
 * Broadcasts on: /topic/session/{id}/cursor
 *                /topic/session/{id}/edit
 *                /topic/session/{id}/participants
 */
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final CollabService collabService;
    private final SimpMessagingTemplate messagingTemplate;

    /** Cursor position update — persists to DB and broadcasts to all session subscribers. */
    @MessageMapping("/session/{sessionId}/cursor")
    public void handleCursorUpdate(@DestinationVariable String sessionId,
                                   @Payload CursorMessage message) {
        collabService.updateCursor(sessionId, message.getUserEmail(), message.getLine(), message.getCol());
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/cursor", message);
    }

    /**
     * Code edit event — broadcasts the delta to all other participants.
     * In a production system, Operational Transformation / CRDT resolution
     * would be applied here before broadcasting.
     */
    @MessageMapping("/session/{sessionId}/edit")
    public void handleCodeEdit(@DestinationVariable String sessionId,
                               @Payload EditMessage message) {
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/edit", message);
    }

    /** Participant join announcement — triggers a participant-list refresh for everyone. */
    @MessageMapping("/session/{sessionId}/join")
    public void handleJoin(@DestinationVariable String sessionId,
                           @Payload ParticipantEvent event) {
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/participants", event);
    }

    /** Participant leave announcement. */
    @MessageMapping("/session/{sessionId}/leave")
    public void handleLeave(@DestinationVariable String sessionId,
                            @Payload ParticipantEvent event) {
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/participants", event);
    }

    // ─── Message payload types ────────────────────────────────────────────────

    @Data
    public static class CursorMessage {
        private String userEmail;
        private String color;
        private int line;
        private int col;
    }

    @Data
    public static class EditMessage {
        private String userEmail;
        private String delta;       // JSON string representing the code delta
        private String content;     // Optional: full content for initial sync
        private long timestamp;
    }

    @Data
    public static class ParticipantEvent {
        private String userEmail;
        private String action;      // "joined" | "left" | "kicked"
        private String color;
    }
}
