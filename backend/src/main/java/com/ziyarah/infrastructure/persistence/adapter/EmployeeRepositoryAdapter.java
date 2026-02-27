package com.ziyarah.infrastructure.persistence.adapter;

import com.ziyarah.domain.entity.Employee;
import com.ziyarah.domain.enums.EmployeeLevel;
import com.ziyarah.domain.repository.EmployeeRepository;
import com.ziyarah.infrastructure.persistence.entity.EmployeeJpaEntity;
import com.ziyarah.infrastructure.persistence.mapper.EmployeeMapper;
import com.ziyarah.infrastructure.persistence.repository.EmployeeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository Adapter: EmployeeRepositoryAdapter
 */
@Component
@RequiredArgsConstructor
public class EmployeeRepositoryAdapter implements EmployeeRepository {
    
    private final EmployeeJpaRepository employeeJpaRepository;
    private final EmployeeMapper employeeMapper;
    
    @Override
    public Employee save(Employee employee) {
        EmployeeJpaEntity entity = employeeMapper.toJpaEntity(employee);
        EmployeeJpaEntity savedEntity = employeeJpaRepository.save(entity);
        return employeeMapper.toDomainEntity(savedEntity);
    }
    
    @Override
    public Optional<Employee> findById(UUID id) {
        return employeeJpaRepository.findById(id)
                .map(employeeMapper::toDomainEntity);
    }
    
    @Override
    public Optional<Employee> findByUserId(UUID userId) {
        return employeeJpaRepository.findByUserId(userId)
                .map(employeeMapper::toDomainEntity);
    }
    
    @Override
    public Optional<Employee> findByEmployeeId(String employeeId) {
        return employeeJpaRepository.findByEmployeeId(employeeId)
                .map(employeeMapper::toDomainEntity);
    }
    
    @Override
    public List<Employee> findByDepartmentId(UUID departmentId) {
        return employeeJpaRepository.findByDepartmentId(departmentId).stream()
                .map(employeeMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Employee> findByLevel(EmployeeLevel level) {
        return employeeJpaRepository.findByLevel(level).stream()
                .map(employeeMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Employee> findAll() {
        return employeeJpaRepository.findAll().stream()
                .map(employeeMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(UUID id) {
        employeeJpaRepository.deleteById(id);
    }
    
    @Override
    public boolean existsByEmployeeId(String employeeId) {
        return employeeJpaRepository.existsByEmployeeId(employeeId);
    }
}
