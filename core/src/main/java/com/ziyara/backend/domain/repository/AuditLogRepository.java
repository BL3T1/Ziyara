package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogRepository {
    AuditLog save(AuditLog auditLog);

    Optional<AuditLog> findById(UUID id);

    List<AuditLog> findByUserId(UUID userId);

    List<AuditLog> findByEntityNameAndEntityId(String entityName, String entityId);

    Page<AuditLog> findRecent(Pageable pageable);

    /**
     * Server-side filtered query. All parameters are optional (null = no filter).
     * Used by the deletion-log component and general audit-log search.
     */
    Page<AuditLog> findFiltered(String entityType,
                                String action,
                                UUID userId,
                                LocalDateTime dateFrom,
                                LocalDateTime dateTo,
                                Pageable pageable);
}
