package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.RestMenuSection;
import com.ziyara.backend.infrastructure.persistence.entity.RestMenuSectionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class RestMenuSectionMapper {

    public RestMenuSection toDomainEntity(RestMenuSectionJpaEntity entity) {
        if (entity == null) return null;
        RestMenuSection section = new RestMenuSection();
        section.setId(entity.getId());
        section.setServiceId(entity.getServiceId());
        section.setTitle(entity.getTitle());
        section.setSortOrder(entity.getSortOrder());
        section.setCreatedAt(entity.getCreatedAt());
        section.setUpdatedAt(entity.getUpdatedAt());
        return section;
    }

    public RestMenuSectionJpaEntity toJpaEntity(RestMenuSection section) {
        if (section == null) return null;
        return RestMenuSectionJpaEntity.builder()
                .id(section.getId())
                .serviceId(section.getServiceId())
                .title(section.getTitle())
                .sortOrder(section.getSortOrder())
                .createdAt(section.getCreatedAt())
                .updatedAt(section.getUpdatedAt())
                .build();
    }
}
