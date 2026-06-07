package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.ProviderSubscriptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderSubscriptionJpaRepository extends JpaRepository<ProviderSubscriptionJpaEntity, UUID> {
    Optional<ProviderSubscriptionJpaEntity> findByProviderId(UUID providerId);
}
