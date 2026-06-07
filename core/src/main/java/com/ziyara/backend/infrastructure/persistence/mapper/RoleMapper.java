package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.Role;
import com.ziyara.backend.domain.enums.RoleStatus;
import com.ziyara.backend.infrastructure.persistence.entity.RoleJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {

    public Role toDomainEntity(RoleJpaEntity entity) {
        if (entity == null) return null;
        Role r = new Role();
        r.setId(entity.getId());
        r.setName(entity.getName());
        r.setNameAr(entity.getNameAr());
        r.setCode(entity.getCode());
        r.setDescription(entity.getDescription());
        r.setDescriptionAr(entity.getDescriptionAr());
        r.setLevel(entity.getLevel());
        r.setGroupId(entity.getGroupId());
        r.setSystemRole(Boolean.TRUE.equals(entity.getSystemRole()));
        r.setStatus(entity.getStatus() != null ? entity.getStatus() : RoleStatus.ACTIVE);
        r.setCreatedAt(entity.getCreatedAt());
        r.setUpdatedAt(entity.getUpdatedAt());
        r.setNavigationItemIds(entity.getNavigationItemIds());
        r.setMaxDiscountPct(entity.getMaxDiscountPct());
        return r;
    }

    public RoleJpaEntity toJpaEntity(Role domain) {
        if (domain == null) return null;
        return RoleJpaEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .nameAr(domain.getNameAr())
                .code(domain.getCode())
                .description(domain.getDescription())
                .descriptionAr(domain.getDescriptionAr())
                .level(domain.getLevel())
                .groupId(domain.getGroupId())
                .systemRole(domain.isSystemRole())
                .status(domain.getStatus() != null ? domain.getStatus() : RoleStatus.ACTIVE)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .navigationItemIds(domain.getNavigationItemIds())
                .maxDiscountPct(domain.getMaxDiscountPct())
                .build();
    }
}
