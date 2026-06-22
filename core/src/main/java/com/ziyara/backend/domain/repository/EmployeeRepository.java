package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.Employee;
import com.ziyara.backend.domain.enums.EmployeeLevel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository Port: EmployeeRepository
 */
public interface EmployeeRepository {
    Employee save(Employee employee);
    Optional<Employee> findById(UUID id);
    Optional<Employee> findByUserId(UUID userId);
    Optional<Employee> findByEmployeeId(String employeeId);
    List<Employee> findByDepartmentId(UUID departmentId);
    List<Employee> findByLevel(EmployeeLevel level);
    /** All employees including offboarded ones (for audit/history views). */
    List<Employee> findAll();
    /** Active employees only (offboardedAt IS NULL). */
    List<Employee> findAllActive();
    /** Count of active employees — used for seat-limit enforcement. */
    long countActive();
    void deleteById(UUID id);
    boolean existsByEmployeeId(String employeeId);
}
