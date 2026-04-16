package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.FeatureFlagJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FeatureFlagJpaRepository extends JpaRepository<FeatureFlagJpaEntity, UUID> {

    Optional<FeatureFlagJpaEntity> findByFlagKey(String flagKey);
}
