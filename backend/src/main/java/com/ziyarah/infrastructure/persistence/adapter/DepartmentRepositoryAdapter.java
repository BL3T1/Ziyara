package com.ziyarah.infrastructure.persistence.adapter;

import com.ziyarah.domain.entity.Department;
import com.ziyarah.domain.repository.DepartmentRepository;
import com.ziyarah.infrastructure.persistence.entity.DepartmentJpaEntity;
import com.ziyarah.infrastructure.persistence.mapper.DepartmentMapper;
import com.ziyarah.infrastructure.persistence.repository.DepartmentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository Adapter: DepartmentRepositoryAdapter
 */
@Component
@RequiredArgsConstructor
public class DepartmentRepositoryAdapter implements DepartmentRepository {
    
    private final DepartmentJpaRepository departmentJpaRepository;
    private final DepartmentMapper departmentMapper;
    
    @Override
    public Department save(Department department) {
        DepartmentJpaEntity entity = departmentMapper.toJpaEntity(department);
        DepartmentJpaEntity savedEntity = departmentJpaRepository.save(entity);
        return departmentMapper.toDomainEntity(savedEntity);
    }
    
    @Override
    public Optional<Department> findById(UUID id) {
        return departmentJpaRepository.findById(id)
                .map(departmentMapper::toDomainEntity);
    }
    
    @Override
    public Optional<Department> findByName(String name) {
        return departmentJpaRepository.findByName(name)
                .map(departmentMapper::toDomainEntity);
    }
    
    @Override
    public List<Department> findAll() {
        return departmentJpaRepository.findAll().stream()
                .map(departmentMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(UUID id) {
        departmentJpaRepository.deleteById(id);
    }
}
