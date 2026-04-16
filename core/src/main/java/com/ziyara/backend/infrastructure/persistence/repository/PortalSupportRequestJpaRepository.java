package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.PortalSupportRequestJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PortalSupportRequestJpaRepository extends JpaRepository<PortalSupportRequestJpaEntity, UUID> {

    List<PortalSupportRequestJpaEntity> findByProviderIdOrderByCreatedAtDesc(UUID providerId);
}
