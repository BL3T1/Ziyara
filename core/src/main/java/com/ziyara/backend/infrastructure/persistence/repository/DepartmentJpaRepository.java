package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.DepartmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository: DepartmentJpaRepository
 */
@Repository
public interface DepartmentJpaRepository extends JpaRepository<DepartmentJpaEntity, UUID> {
    Optional<DepartmentJpaEntity> findByName(String name);
}
