package com.ziyarah.domain.repository;

import com.ziyarah.domain.entity.AuditLog;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogRepository {
    AuditLog save(AuditLog auditLog);
    Optional<AuditLog> findById(UUID id);
    List<AuditLog> findByUserId(UUID userId);
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId);
}
