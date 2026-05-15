/*
 * Code reader note: Runs startup database migration SQL needed by project-service.
 */
package com.codesync.project.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Runs once on startup (after JPA/Hibernate schema update) to apply any
 * schema corrections that Hibernate's ddl-auto=update cannot handle.
 *
 * Specifically: makes project_members.user_id nullable (or drops it) so that
 * email-based member inserts (which never supply a user_id) succeed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaMigrationRunner {

    private final JdbcTemplate jdbc;

    @PostConstruct
    public void runMigrations() {
        fixProjectMembersUserId();
    }

    /**
     * The project_members table may contain a legacy user_id column with
     * a NOT NULL constraint from an earlier schema version that used numeric
     * user IDs. The current entity model uses userEmail instead, so the
     * user_id column is never populated. This migration makes it nullable
     * to prevent constraint violations on INSERT.
     */
    private void fixProjectMembersUserId() {
        try {
            // Check if the user_id column exists AND is currently NOT NULL
            Integer notNullCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_name = 'project_members' " +
                "  AND column_name = 'user_id' " +
                "  AND is_nullable = 'NO'",
                Integer.class
            );

            if (notNullCount != null && notNullCount > 0) {
                jdbc.execute("ALTER TABLE project_members ALTER COLUMN user_id DROP NOT NULL");
                log.info("✅ Migration applied: project_members.user_id is now nullable.");
            } else {
                log.debug("Migration check: project_members.user_id is already nullable or does not exist — skipping.");
            }
        } catch (Exception e) {
            // Non-fatal: log and continue. The application can still start.
            log.warn("Schema migration warning (project_members.user_id nullable): {}", e.getMessage());
        }
    }
}
