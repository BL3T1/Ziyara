package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.PiiFieldRegistryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PiiFieldRegistryJpaRepository extends JpaRepository<PiiFieldRegistryJpaEntity, UUID> {
}
