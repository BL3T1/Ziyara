package com.ziyarah.infrastructure.persistence.mapper;

import com.ziyarah.domain.entity.Department;
import com.ziyarah.infrastructure.persistence.entity.DepartmentJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper: DepartmentMapper
 */
@Component
public class DepartmentMapper {
    
    public Department toDomainEntity(DepartmentJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        Department department = new Department();
        department.setId(entity.getId());
        department.setName(entity.getName());
        department.setDescription(entity.getDescription());
        department.setManagerId(entity.getManagerId());
        department.setCreatedAt(entity.getCreatedAt());
        department.setUpdatedAt(entity.getUpdatedAt());
        
        return department;
    }
    
    public DepartmentJpaEntity toJpaEntity(Department department) {
        if (department == null) {
            return null;
        }
        
        return DepartmentJpaEntity.builder()
                .id(department.getId())
                .name(department.getName())
                .description(department.getDescription())
                .managerId(department.getManagerId())
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt())
                .build();
    }
}
