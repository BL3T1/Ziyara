package com.ziyara.backend.modules.sys.api;

import com.ziyara.backend.application.dto.response.AuditLogResponse;

import java.util.List;
import java.util.UUID;

/**
 * Sys module API: audit logging (Phase 3). Other modules use this to log actions, not the concrete service.
 */
public interface AuditServiceApi {

    void logAction(String action, String entityName, String entityId, UUID userId,
                   String oldVal, String newVal, String ip, String ua);

    List<AuditLogResponse> getEntityLogs(String entityName, String entityId);

    List<AuditLogResponse> getUserLogs(UUID userId);

    List<AuditLogResponse> getRecentLogs(int limit, String search);
}
