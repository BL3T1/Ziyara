package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.PortalSupportRequestJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PortalSupportRequestJpaRepository extends JpaRepository<PortalSupportRequestJpaEntity, UUID> {

    List<PortalSupportRequestJpaEntity> findByProviderIdOrderByCreatedAtDesc(UUID providerId);

    List<PortalSupportRequestJpaEntity> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("UPDATE PortalSupportRequestJpaEntity r SET r.staffResponse = :response, r.respondedAt = :respondedAt, r.respondedByUserId = :respondedBy WHERE r.id = :id")
    int updateResponse(@Param("id") UUID id,
                       @Param("response") String response,
                       @Param("respondedAt") Instant respondedAt,
                       @Param("respondedBy") UUID respondedBy);
}
