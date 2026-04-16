package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.Department;
import com.ziyara.backend.infrastructure.persistence.entity.DepartmentJpaEntity;
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
        department.setNameAr(entity.getNameAr());
        department.setDescription(entity.getDescription());
        department.setDescriptionAr(entity.getDescriptionAr());
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
                .nameAr(department.getNameAr())
                .description(department.getDescription())
                .descriptionAr(department.getDescriptionAr())
                .managerId(department.getManagerId())
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt())
                .build();
    }
}
