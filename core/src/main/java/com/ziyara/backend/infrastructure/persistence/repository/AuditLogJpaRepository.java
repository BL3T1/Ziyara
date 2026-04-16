package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.AuditLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository: AuditLogJpaRepository
 */
@Repository
public interface AuditLogJpaRepository extends JpaRepository<AuditLogJpaEntity, UUID> {
    List<AuditLogJpaEntity> findByEntityNameAndEntityId(String entityName, String entityId);
    List<AuditLogJpaEntity> findByUserId(UUID userId);
    Page<AuditLogJpaEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
