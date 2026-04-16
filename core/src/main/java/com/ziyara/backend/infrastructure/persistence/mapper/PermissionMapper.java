package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.Permission;
import com.ziyara.backend.infrastructure.persistence.entity.PermissionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PermissionMapper {

    public Permission toDomainEntity(PermissionJpaEntity entity) {
        if (entity == null) return null;
        Permission p = new Permission();
        p.setId(entity.getId());
        p.setCode(entity.getCode());
        p.setName(entity.getName());
        p.setNameAr(entity.getNameAr());
        p.setDescription(entity.getDescription());
        p.setDescriptionAr(entity.getDescriptionAr());
        p.setResource(entity.getResource());
        p.setAction(entity.getAction());
        p.setScope(entity.getScope());
        p.setLocked(Boolean.TRUE.equals(entity.getIsLocked()));
        return p;
    }
}
