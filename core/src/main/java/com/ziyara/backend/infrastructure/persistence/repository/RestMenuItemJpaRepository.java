package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.RestMenuItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RestMenuItemJpaRepository extends JpaRepository<RestMenuItemJpaEntity, UUID> {

    List<RestMenuItemJpaEntity> findBySectionIdOrderBySortOrderAscIdAsc(UUID sectionId);

    long countBySectionId(UUID sectionId);
}
