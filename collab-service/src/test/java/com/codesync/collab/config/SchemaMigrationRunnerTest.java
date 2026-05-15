package com.codesync.collab.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class SchemaMigrationRunnerTest {

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private SchemaMigrationRunner runner;

    @Test
    void testRunMigrations_Applies() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        runner.runMigrations();
        verify(jdbc).execute("ALTER TABLE collab_participants ALTER COLUMN user_id DROP NOT NULL");
    }

    @Test
    void testRunMigrations_Skips() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(0);
        runner.runMigrations();
    }
}
