package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.IntegrationApiKeyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IntegrationApiKeyJpaRepository extends JpaRepository<IntegrationApiKeyJpaEntity, UUID> {

    List<IntegrationApiKeyJpaEntity> findByRevokedAtIsNullOrderByCreatedAtDesc();
}
