package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogRepository {
    AuditLog save(AuditLog auditLog);

    Optional<AuditLog> findById(UUID id);

    List<AuditLog> findByUserId(UUID userId);

    List<AuditLog> findByEntityNameAndEntityId(String entityName, String entityId);

    Page<AuditLog> findRecent(Pageable pageable);
}
