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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Idempotent G{n}→Z{n} rename on canonical platform {@code sys_groups} rows (by UUID) plus {@code sys_user_roles.group_id} repair.
 * <p>
 * The Spring Boot app does not ship Flyway; Docker/legacy DBs may still hold {@code G1}…{@code G7} from older seeds.
 * If a {@code Z*} code is held by a non-canonical row, that row is renamed first (query uses {@code id <> canonical} so the
 * canonical row is never relabeled). Missing canonical rows are inserted so {@link PlatformRbacCatalogValidator} can run.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class PlatformOrgGroupCodeMigrationRunner implements ApplicationRunner {

    private static final Map<String, String[]> SEED = Map.ofEntries(
            Map.entry("Z1", new String[]{"Executive", "Executive group"}),
            Map.entry("Z2", new String[]{"Sales", "Sales group"}),
            Map.entry("Z3", new String[]{"Finance", "Finance group"}),
            Map.entry("Z4", new String[]{"Support", "Customer support and service operations"}),
            Map.entry("Z5", new String[]{"HR & People", "Human resources and internal people operations"}),
            Map.entry("Z6", new String[]{"Provider Partner", "Partner and provider-facing accounts"}),
            Map.entry("Z7", new String[]{"B2C Customers", "End-customer accounts (bookings and profile)"})
    );

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        int groupsRenamed = 0;
        for (PlatformOrgGroups.PlatformGroupSlice slice : PlatformOrgGroups.slices()) {
            UUID canonicalId = slice.id();
            String zCode = slice.code();
            List<UUID> conflicts = jdbcTemplate.query(
                    "SELECT id FROM sys_groups WHERE code = ? AND id <> ? LIMIT 1",
                    (rs, rowNum) -> rs.getObject("id", UUID.class),
                    zCode,
                    canonicalId);
            if (!conflicts.isEmpty()) {
                UUID conflictId = conflicts.get(0);
                String tempCode = allocateUnusedTempCode(conflictId);
                jdbcTemplate.update("UPDATE sys_groups SET code = ? WHERE id = ?", tempCode, conflictId);
                log.warn("Renamed duplicate org group code {} away from non-canonical id {} to {} before platform migration",
                        zCode, conflictId, tempCode);
            }
            groupsRenamed += jdbcTemplate.update(
                    "UPDATE sys_groups SET code = ? WHERE id = ? AND code ~ '^G[0-9]+$'",
                    zCode,
                    canonicalId);
        }
        int userRolesFixed = jdbcTemplate.update(
                "UPDATE sys_user_roles ur SET group_id = r.group_id FROM sys_roles r "
                        + "WHERE ur.role_id = r.id AND (ur.group_id IS DISTINCT FROM r.group_id)");

        int inserted = 0;
        for (PlatformOrgGroups.PlatformGroupSlice slice : PlatformOrgGroups.slices()) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*)::int FROM sys_groups WHERE id = ?",
                    Integer.class,
                    slice.id());
            if (count != null && count == 0) {
                String[] meta = SEED.get(slice.code());
                if (meta == null) {
                    throw new IllegalStateException("Missing seed metadata for platform group " + slice.code());
                }
                jdbcTemplate.update(
                        "INSERT INTO sys_groups (id, name, code, description, created_at, updated_at) "
                                + "VALUES (?, ?, ?, ?, now(), now())",
                        slice.id(),
                        meta[0],
                        slice.code(),
                        meta[1]);
                inserted++;
                log.warn("Inserted missing platform sys_groups row id={} code={}", slice.id(), slice.code());
            }
        }

        if (groupsRenamed > 0 || userRolesFixed > 0 || inserted > 0) {
            log.info("Platform org group migration: {} G→Z renames, {} user_role group_id fixes, {} missing rows inserted",
                    groupsRenamed, userRolesFixed, inserted);
        }
    }

    private String allocateUnusedTempCode(UUID conflictId) {
        String hex = conflictId.toString().replace("-", "").toUpperCase(Locale.ROOT);
        for (int take = 19; take >= 6; take--) {
            String candidate = ("D" + hex.substring(0, Math.min(hex.length(), take)));
            if (candidate.length() > 20) {
                candidate = candidate.substring(0, 20);
            }
            Integer c = jdbcTemplate.queryForObject("SELECT COUNT(*)::int FROM sys_groups WHERE code = ?", Integer.class, candidate);
            if (c != null && c == 0) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate unique temp code for conflicting org group " + conflictId);
    }
}
