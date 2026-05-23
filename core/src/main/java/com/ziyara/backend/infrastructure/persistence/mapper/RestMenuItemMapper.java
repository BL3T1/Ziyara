package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.RestMenuItem;
import com.ziyara.backend.infrastructure.persistence.entity.RestMenuItemJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class RestMenuItemMapper {

    public RestMenuItem toDomainEntity(RestMenuItemJpaEntity entity) {
        if (entity == null) return null;
        RestMenuItem item = new RestMenuItem();
        item.setId(entity.getId());
        item.setSectionId(entity.getSectionId());
        item.setName(entity.getName());
        item.setDescription(entity.getDescription());
        item.setPrice(entity.getPrice());
        item.setCurrency(entity.getCurrency());
        item.setImageUrl(entity.getImageUrl());
        item.setSortOrder(entity.getSortOrder());
        item.setCreatedAt(entity.getCreatedAt());
        item.setUpdatedAt(entity.getUpdatedAt());
        return item;
    }

    public RestMenuItemJpaEntity toJpaEntity(RestMenuItem item) {
        if (item == null) return null;
        return RestMenuItemJpaEntity.builder()
                .id(item.getId())
                .sectionId(item.getSectionId())
                .name(item.getName())
                .description(item.getDescription())
                .price(item.getPrice())
                .currency(item.getCurrency())
                .imageUrl(item.getImageUrl())
                .sortOrder(item.getSortOrder())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
