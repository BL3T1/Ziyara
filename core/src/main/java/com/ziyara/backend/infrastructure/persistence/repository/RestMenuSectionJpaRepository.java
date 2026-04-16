package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.RestMenuSectionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RestMenuSectionJpaRepository extends JpaRepository<RestMenuSectionJpaEntity, UUID> {

    List<RestMenuSectionJpaEntity> findByServiceIdOrderBySortOrderAscIdAsc(UUID serviceId);

    long countByServiceId(UUID serviceId);
}
