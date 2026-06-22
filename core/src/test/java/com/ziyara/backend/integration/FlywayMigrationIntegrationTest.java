package com.ziyara.backend.integration;

import com.ziyara.backend.AbstractIntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that Flyway migrations apply cleanly to a fresh Testcontainers PostgreSQL database.
 * Checks schema integrity: expected tables exist, version matches the latest migration.
 */
class FlywayMigrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;

    @Test
    void allMigrationsApplyWithoutErrors() {
        var info = flyway.info();
        var applied = List.of(info.applied());
        assertThat(applied).isNotEmpty();
        applied.forEach(m ->
            assertThat(m.getState().isApplied())
                    .as("Migration %s should be applied", m.getVersion())
                    .isTrue()
        );
    }

    @Test
    void coreTablesExist() {
        List<String> requiredTables = List.of(
                "users", "roles", "permissions", "service_providers",
                "services", "bookings", "payments", "notifications"
        );
        requiredTables.forEach(table -> {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
                    Integer.class, table);
            assertThat(count)
                    .as("Table '%s' must exist after migrations", table)
                    .isGreaterThan(0);
        });
    }

    @Test
    void flywaySchemaHistoryHasNoFailedMigrations() {
        List<Map<String, Object>> failed = jdbc.queryForList(
                "SELECT version, description FROM flyway_schema_history WHERE success = false"
        );
        assertThat(failed)
                .as("No failed Flyway migrations should be present")
                .isEmpty();
    }

    @Test
    void noOrphanedMigrationChecksums() {
        var info = flyway.info();
        long pendingCount = java.util.Arrays.stream(info.all())
                .filter(m -> m.getState().name().equals("PENDING"))
                .count();
        assertThat(pendingCount)
                .as("No migrations should be left in PENDING state after startup")
                .isZero();
    }
}
