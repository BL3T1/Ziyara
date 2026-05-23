package com.ziyara.backend.application.service;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Records privileged admin actions to sys_admin_activity_log.
 */
@Service
@RequiredArgsConstructor
public class AdminActivityLogService {

    private static final String TABLE = "sys_admin_activity_log";

    private final DSLContext dsl;

    public void log(UUID adminId, String action, String targetType, String targetId,
                    String description, String ipAddress, String userAgent) {
        dsl.insertInto(DSL.table(DSL.name(TABLE)))
                .columns(
                        DSL.field(DSL.name("id")),
                        DSL.field(DSL.name("admin_id")),
                        DSL.field(DSL.name("action")),
                        DSL.field(DSL.name("target_type")),
                        DSL.field(DSL.name("target_id")),
                        DSL.field(DSL.name("description")),
                        DSL.field(DSL.name("ip_address")),
                        DSL.field(DSL.name("user_agent")),
                        DSL.field(DSL.name("performed_at"))
                )
                .values(
                        UUID.randomUUID(),
                        adminId,
                        action,
                        targetType,
                        targetId,
                        description,
                        ipAddress,
                        userAgent,
                        OffsetDateTime.now()
                )
                .execute();
    }

    public void log(UUID adminId, String action, String targetType, String targetId, String description) {
        log(adminId, action, targetType, targetId, description, null, null);
    }
}
