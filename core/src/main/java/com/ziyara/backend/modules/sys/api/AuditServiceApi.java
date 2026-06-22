package com.ziyara.backend.modules.sys.api;

import com.ziyara.backend.application.dto.response.AuditLogResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
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

    /**
     * Filtered log query — powers the deletion-log component and general audit search.
     * All filter parameters are optional (null = no filter on that dimension).
     */
    Page<AuditLogResponse> getFilteredLogs(String entityType,
                                            String action,
                                            UUID userId,
                                            LocalDateTime dateFrom,
                                            LocalDateTime dateTo,
                                            int page,
                                            int size);
}
