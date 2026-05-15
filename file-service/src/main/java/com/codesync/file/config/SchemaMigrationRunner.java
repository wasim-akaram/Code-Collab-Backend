/*
 * Code reader note: Runs startup database migration SQL needed by file-service.
 */
package com.codesync.file.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Runs once on startup (after JPA/Hibernate schema update) to apply any
 * schema corrections that Hibernate's ddl-auto=update cannot handle.
 *
 * Specifically: makes code_files.user_id nullable (if the column exists)
 * so that the current entity model (which uses createdBy/lastEditedBy
 * instead of user_id) can insert rows without violating NOT NULL constraints.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaMigrationRunner {

    private final JdbcTemplate jdbc;

    @PostConstruct
    public void runMigrations() {
        fixCodeFilesUserId();
    }

    private void fixCodeFilesUserId() {
        try {
            Integer notNullCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_name = 'code_files' " +
                "  AND column_name = 'user_id' " +
                "  AND is_nullable = 'NO'",
                Integer.class
            );

            if (notNullCount != null && notNullCount > 0) {
                jdbc.execute("ALTER TABLE code_files ALTER COLUMN user_id DROP NOT NULL");
                log.info("✅ Migration applied: code_files.user_id is now nullable.");
            } else {
                log.debug("Migration check: code_files.user_id is already nullable or does not exist — skipping.");
            }
        } catch (Exception e) {
            log.warn("Schema migration warning (code_files.user_id): {}", e.getMessage());
        }
    }
}
