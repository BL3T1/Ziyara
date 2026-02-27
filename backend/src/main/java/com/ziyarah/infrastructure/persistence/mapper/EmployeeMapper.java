package com.ziyarah.infrastructure.persistence.mapper;

import com.ziyarah.domain.entity.Employee;
import com.ziyarah.infrastructure.persistence.entity.EmployeeJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper: EmployeeMapper
 * Maps between domain Employee entity and JPA EmployeeJpaEntity
 */
@Component
public class EmployeeMapper {
    
    public Employee toDomainEntity(EmployeeJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        Employee employee = new Employee();
        employee.setId(entity.getId());
        employee.setUserId(entity.getUserId());
        employee.setDepartmentId(entity.getDepartmentId());
        employee.setEmployeeId(entity.getEmployeeId());
        employee.setLevel(entity.getLevel());
        employee.setDesignation(entity.getDesignation());
        employee.setJoiningDate(entity.getJoiningDate());
        employee.setCreatedAt(entity.getCreatedAt());
        employee.setUpdatedAt(entity.getUpdatedAt());
        
        return employee;
    }
    
    public EmployeeJpaEntity toJpaEntity(Employee employee) {
        if (employee == null) {
            return null;
        }
        
        return EmployeeJpaEntity.builder()
                .id(employee.getId())
                .userId(employee.getUserId())
                .departmentId(employee.getDepartmentId())
                .employeeId(employee.getEmployeeId())
                .level(employee.getLevel())
                .designation(employee.getDesignation())
                .joiningDate(employee.getJoiningDate())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }
}
