package com.codesync.collab.controller;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.codesync.collab.service.CollabService;

@ExtendWith(MockitoExtension.class)
class WebSocketControllerTest {

    @Mock
    private CollabService collabService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketController webSocketController;

    @Test
    void testHandleCursorUpdate() {
        WebSocketController.CursorMessage msg = new WebSocketController.CursorMessage();
        msg.setUserEmail("test@test.com");
        msg.setLine(10);
        msg.setCol(20);

        webSocketController.handleCursorUpdate("session1", msg);

        verify(collabService).updateCursor("session1", "test@test.com", 10, 20);
        verify(messagingTemplate).convertAndSend("/topic/session/session1/cursor", msg);
    }

    @Test
    void testHandleCodeEdit() {
        WebSocketController.EditMessage msg = new WebSocketController.EditMessage();
        msg.setUserEmail("test@test.com");

        webSocketController.handleCodeEdit("session1", msg);

        verify(messagingTemplate).convertAndSend("/topic/session/session1/edit", msg);
    }

    @Test
    void testHandleJoin() {
        WebSocketController.ParticipantEvent msg = new WebSocketController.ParticipantEvent();
        msg.setUserEmail("test@test.com");

        webSocketController.handleJoin("session1", msg);

        verify(messagingTemplate).convertAndSend("/topic/session/session1/participants", msg);
    }

    @Test
    void testHandleLeave() {
        WebSocketController.ParticipantEvent msg = new WebSocketController.ParticipantEvent();
        msg.setUserEmail("test@test.com");

        webSocketController.handleLeave("session1", msg);

        verify(messagingTemplate).convertAndSend("/topic/session/session1/participants", msg);
    }
}
