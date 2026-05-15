package com.codesync.collab.scheduler;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codesync.collab.service.CollabServiceImpl;

@ExtendWith(MockitoExtension.class)
class SessionCleanupTaskTest {

    @Mock
    private CollabServiceImpl collabService;

    @InjectMocks
    private SessionCleanupTask sessionCleanupTask;

    @Test
    void testCleanupIdleSessions() {
        sessionCleanupTask.cleanupIdleSessions();
        verify(collabService).endIdleSessions();
    }
}
