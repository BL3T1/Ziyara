package com.ziyarah.infrastructure.persistence.repository;

import com.ziyarah.infrastructure.persistence.entity.AuditLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository: AuditLogJpaRepository
 */
@Repository
public interface AuditLogJpaRepository extends JpaRepository<AuditLogJpaEntity, UUID> {
    List<AuditLogJpaEntity> findByEntityNameAndEntityId(String entityName, String entityId);
    List<AuditLogJpaEntity> findByUserId(UUID userId);
}
