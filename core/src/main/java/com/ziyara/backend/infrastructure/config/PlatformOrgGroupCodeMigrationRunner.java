package com.ziyara.backend.infrastructure.config;

import com.ziyara.backend.domain.catalog.PlatformOrgGroups;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Ensures the C1 Admin group exists and removes any legacy Z1–Z7 groups left over on databases
 * that pre-date Flyway V26. Runs before {@link PlatformRbacCatalogValidator}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class PlatformOrgGroupCodeMigrationRunner implements ApplicationRunner {

    private static final UUID C1_ID = UUID.fromString("b0000000-0000-0000-0000-000000000010");

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ensureAdminGroupExists();

        int rolesFixed = jdbcTemplate.update(
                "UPDATE sys_roles SET group_id = ? WHERE group_id IN ("
                        + "'b0000000-0000-0000-0000-000000000001'::uuid,"
                        + "'b0000000-0000-0000-0000-000000000002'::uuid,"
                        + "'b0000000-0000-0000-0000-000000000003'::uuid,"
                        + "'b0000000-0000-0000-0000-000000000004'::uuid,"
                        + "'b0000000-0000-0000-0000-000000000005'::uuid,"
                        + "'b0000000-0000-0000-0000-000000000006'::uuid,"
                        + "'b0000000-0000-0000-0000-000000000007'::uuid)",
                C1_ID);

        int userRolesFixed = jdbcTemplate.update(
                "UPDATE sys_user_roles ur SET group_id = r.group_id FROM sys_roles r "
                        + "WHERE ur.role_id = r.id AND (ur.group_id IS DISTINCT FROM r.group_id)");

        int deleted = jdbcTemplate.update(
                "DELETE FROM sys_groups WHERE id IN ("
                        + "'b0000000-0000-0000-0000-000000000001'::uuid,"
                        + "'b0000000-0000-0000-0000-000000000002'::uuid,"
                        + "'b0000000-0000-0000-0000-000000000003'::uuid,"
                        + "'b0000000-0000-0000-0000-000000000004'::uuid,"
                        + "'b0000000-0000-0000-0000-000000000005'::uuid,"
                        + "'b0000000-0000-0000-0000-000000000006'::uuid,"
                        + "'b0000000-0000-0000-0000-000000000007'::uuid)");

        if (rolesFixed > 0 || userRolesFixed > 0 || deleted > 0) {
            log.info("Platform group migration: {} roles redirected to C1, {} user_role group fixes, {} legacy Z groups removed",
                    rolesFixed, userRolesFixed, deleted);
        }
    }

    private void ensureAdminGroupExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*)::int FROM sys_groups WHERE id = ?", Integer.class, C1_ID);
        if (count == null || count == 0) {
            jdbcTemplate.update(
                    "INSERT INTO sys_groups (id, name, code, description, created_at) VALUES (?, 'Admin', 'C1', 'Platform administrative group', now())",
                    C1_ID);
            log.warn("Inserted missing platform sys_groups row id={} code=C1", C1_ID);
        }
    }
}
