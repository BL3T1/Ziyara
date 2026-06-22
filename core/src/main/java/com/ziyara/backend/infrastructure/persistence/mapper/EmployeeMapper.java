package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.Employee;
import com.ziyara.backend.infrastructure.persistence.entity.EmployeeJpaEntity;
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
        employee.setId(entity.getUserId());
        employee.setUserId(entity.getUserId());
        employee.setDepartmentId(entity.getDepartmentId());
        employee.setEmployeeId(entity.getEmployeeCode());
        employee.setLevel(entity.getLevel());
        employee.setDesignation(entity.getJobTitle());
        employee.setJoiningDate(entity.getHireDate() != null ? entity.getHireDate().atStartOfDay() : null);
        employee.setCreatedAt(entity.getCreatedAt());
        employee.setUpdatedAt(entity.getUpdatedAt());
        employee.setOffboardedAt(entity.getOffboardedAt());
        employee.setOffboardedBy(entity.getOffboardedBy());
        employee.setOffboardReason(entity.getOffboardReason());

        return employee;
    }
    
    public EmployeeJpaEntity toJpaEntity(Employee employee) {
        if (employee == null) {
            return null;
        }
        
        return EmployeeJpaEntity.builder()
                .userId(employee.getUserId())
                .departmentId(employee.getDepartmentId())
                .employeeCode(employee.getEmployeeId())
                .level(employee.getLevel())
                .jobTitle(employee.getDesignation())
                .hireDate(employee.getJoiningDate() != null ? employee.getJoiningDate().toLocalDate() : java.time.LocalDate.now())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .offboardedAt(employee.getOffboardedAt())
                .offboardedBy(employee.getOffboardedBy())
                .offboardReason(employee.getOffboardReason())
                .build();
    }
}
