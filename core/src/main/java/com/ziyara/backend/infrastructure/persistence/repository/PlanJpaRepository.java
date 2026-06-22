package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.PlanJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanJpaRepository extends JpaRepository<PlanJpaEntity, UUID> {
    List<PlanJpaEntity> findByActiveTrue();
    Optional<PlanJpaEntity> findByCode(String code);
}
