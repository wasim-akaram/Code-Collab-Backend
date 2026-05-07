/*
 * Code reader note: Runs scheduled cleanup that closes idle collaboration sessions.
 */
package com.codesync.collab.scheduler;

import com.codesync.collab.service.CollabServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task that automatically ends collaboration sessions with no
 * active participants for 30+ minutes, per the platform spec (Section 2.6).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionCleanupTask {

    private final CollabServiceImpl collabService;

    /**
     * Runs every 5 minutes. Ends all ACTIVE sessions that have had
     * zero participants for at least 30 minutes.
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000) // every 5 minutes
    public void cleanupIdleSessions() {
        log.info("Running idle session cleanup...");
        collabService.endIdleSessions();
    }
}
