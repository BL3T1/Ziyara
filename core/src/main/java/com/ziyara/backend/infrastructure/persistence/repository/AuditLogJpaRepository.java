package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.AuditLogJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    /**
     * Dynamic filter query. String/UUID parameters are nullable (null = no filter).
     * Timestamp parameters must be non-null; callers should pass
     * {@code LocalDateTime.of(1900,1,1,0,0)} and {@code LocalDateTime.of(9999,12,31,23,59)}
     * as open-ended bounds to avoid PostgreSQL parameter type-inference failures
     * ("could not determine data type of parameter $N") on null TIMESTAMPTZ slots.
     */
    @Query("SELECT a FROM AuditLogJpaEntity a WHERE "
         + "(:entityType IS NULL OR a.entityType = :entityType) AND "
         + "(:action     IS NULL OR a.action     = :action)     AND "
         + "(:userId     IS NULL OR a.userId     = :userId)     AND "
         + "a.createdAt >= :dateFrom AND "
         + "a.createdAt <= :dateTo  "
         + "ORDER BY a.createdAt DESC")
    Page<AuditLogJpaEntity> findFiltered(
            @Param("entityType") String entityType,
            @Param("action")     String action,
            @Param("userId")     UUID userId,
            @Param("dateFrom")   LocalDateTime dateFrom,
            @Param("dateTo")     LocalDateTime dateTo,
            Pageable pageable);
}
