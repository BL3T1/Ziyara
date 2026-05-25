package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.common.PageQuery;
import com.ziyara.backend.domain.common.PagedResult;
import com.ziyara.backend.domain.entity.AuditLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogRepository {
    AuditLog save(AuditLog auditLog);

    Optional<AuditLog> findById(UUID id);

    List<AuditLog> findByUserId(UUID userId);

    List<AuditLog> findByEntityNameAndEntityId(String entityName, String entityId);

    PagedResult<AuditLog> findRecent(PageQuery pageQuery);

    /**
     * Server-side filtered query. All parameters are optional (null = no filter).
     * Used by the deletion-log component and general audit-log search.
     */
    PagedResult<AuditLog> findFiltered(String entityType,
                                       String action,
                                       UUID userId,
                                       LocalDateTime dateFrom,
                                       LocalDateTime dateTo,
                                       PageQuery pageQuery);
}
