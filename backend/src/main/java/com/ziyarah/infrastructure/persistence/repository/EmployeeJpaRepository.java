package com.ziyarah.infrastructure.persistence.repository;

import com.ziyarah.domain.enums.EmployeeLevel;
import com.ziyarah.infrastructure.persistence.entity.EmployeeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository: EmployeeJpaRepository
 */
@Repository
public interface EmployeeJpaRepository extends JpaRepository<EmployeeJpaEntity, UUID> {
    Optional<EmployeeJpaEntity> findByUserId(UUID userId);
    Optional<EmployeeJpaEntity> findByEmployeeId(String employeeId);
    List<EmployeeJpaEntity> findByDepartmentId(UUID departmentId);
    List<EmployeeJpaEntity> findByLevel(EmployeeLevel level);
    boolean existsByEmployeeId(String employeeId);
}
