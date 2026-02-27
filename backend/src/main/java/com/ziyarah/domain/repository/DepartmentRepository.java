package com.ziyarah.domain.repository;

import com.ziyarah.domain.entity.Department;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository Port: DepartmentRepository
 */
public interface DepartmentRepository {
    Department save(Department department);
    Optional<Department> findById(UUID id);
    Optional<Department> findByName(String name);
    List<Department> findAll();
    void deleteById(UUID id);
}
