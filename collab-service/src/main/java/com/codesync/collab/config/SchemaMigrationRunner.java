/*
 * Code reader note: Runs startup database migration SQL needed by collab-service.
 */
package com.codesync.collab.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Runs once on startup (after JPA/Hibernate schema update) to apply any
 * schema corrections that Hibernate's ddl-auto=update cannot handle.
 *
 * Specifically: makes collab_participants.user_id nullable so that
 * email-based participant inserts (which never supply a user_id) succeed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaMigrationRunner {

    private final JdbcTemplate jdbc;

    @PostConstruct
    public void runMigrations() {
        try {
            // Check if the column exists AND is currently NOT NULL
            Integer notNullCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_name = 'collab_participants' " +
                "  AND column_name = 'user_id' " +
                "  AND is_nullable = 'NO'",
                Integer.class
            );

            if (notNullCount != null && notNullCount > 0) {
                jdbc.execute("ALTER TABLE collab_participants ALTER COLUMN user_id DROP NOT NULL");
                log.info("✅ Migration applied: collab_participants.user_id is now nullable.");
            } else {
                log.debug("Migration check: collab_participants.user_id is already nullable or does not exist — skipping.");
            }
        } catch (Exception e) {
            // Non-fatal: log and continue. The application can still start.
            log.warn("Schema migration warning (user_id nullable): {}", e.getMessage());
        }
    }
}
