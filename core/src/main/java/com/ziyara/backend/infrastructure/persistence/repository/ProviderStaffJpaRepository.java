package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.ProviderStaffJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderStaffJpaRepository extends JpaRepository<ProviderStaffJpaEntity, UUID> {

    List<ProviderStaffJpaEntity> findByProviderIdOrderByCreatedAtAsc(UUID providerId);

    long countByProviderId(UUID providerId);

    Optional<ProviderStaffJpaEntity> findByProviderIdAndUserId(UUID providerId, UUID userId);

    Optional<ProviderStaffJpaEntity> findByUserId(UUID userId);
}
