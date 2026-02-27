package com.ziyarah.domain.repository;

import com.ziyarah.domain.entity.Employee;
import com.ziyarah.domain.enums.EmployeeLevel;
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
    List<Employee> findAll();
    void deleteById(UUID id);
    boolean existsByEmployeeId(String employeeId);
}
