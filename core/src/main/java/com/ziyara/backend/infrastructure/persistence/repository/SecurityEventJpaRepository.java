package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.SecurityEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface SecurityEventJpaRepository extends JpaRepository<SecurityEventJpaEntity, UUID> {

    @Query("SELECT COUNT(e) FROM SecurityEventJpaEntity e WHERE e.eventType = :type AND e.ipAddress = :ip AND e.createdAt >= :since")
    long countByTypeAndIpSince(@Param("type") String type, @Param("ip") String ip, @Param("since") Instant since);
}
