package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.Group;
import com.ziyara.backend.infrastructure.persistence.entity.GroupJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class GroupMapper {

    public Group toDomainEntity(GroupJpaEntity entity) {
        if (entity == null) return null;
        Group g = new Group();
        g.setId(entity.getId());
        g.setName(entity.getName());
        g.setNameAr(entity.getNameAr());
        g.setCode(entity.getCode());
        g.setDescription(entity.getDescription());
        g.setDescriptionAr(entity.getDescriptionAr());
        g.setCreatedAt(entity.getCreatedAt());
        g.setUpdatedAt(entity.getUpdatedAt());
        return g;
    }

    public GroupJpaEntity toJpaEntity(Group domain) {
        if (domain == null) {
            return null;
        }
        return GroupJpaEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .nameAr(domain.getNameAr())
                .code(domain.getCode())
                .description(domain.getDescription())
                .descriptionAr(domain.getDescriptionAr())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
