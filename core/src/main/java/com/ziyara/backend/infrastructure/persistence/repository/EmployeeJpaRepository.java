package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.domain.enums.EmployeeLevel;
import com.ziyara.backend.infrastructure.persistence.entity.EmployeeJpaEntity;
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
    Optional<EmployeeJpaEntity> findByEmployeeCode(String employeeCode);
    List<EmployeeJpaEntity> findByDepartmentId(UUID departmentId);
    List<EmployeeJpaEntity> findByLevel(EmployeeLevel level);
    boolean existsByEmployeeCode(String employeeCode);

    /** Active employees only (not offboarded). */
    List<EmployeeJpaEntity> findByOffboardedAtIsNull();

    /** Count of active employees — for seat-limit checks. */
    long countByOffboardedAtIsNull();
}
