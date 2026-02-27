package com.ziyarah.infrastructure.persistence.mapper;

import com.ziyarah.domain.entity.ServiceImage;
import com.ziyarah.infrastructure.persistence.entity.ServiceImageJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper: ServiceImageMapper
 */
@Component
public class ServiceImageMapper {
    
    public ServiceImage toDomainEntity(ServiceImageJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        ServiceImage image = new ServiceImage();
        image.setId(entity.getId());
        image.setServiceId(entity.getServiceId());
        image.setUrl(entity.getUrl());
        image.setAltText(entity.getAltText());
        image.setPrimary(entity.getIsPrimary() != null && entity.getIsPrimary());
        image.setDisplayOrder(entity.getDisplayOrder() != null ? entity.getDisplayOrder() : 0);
        image.setCreatedAt(entity.getCreatedAt());
        image.setUpdatedAt(entity.getUpdatedAt());
        
        return image;
    }
    
    public ServiceImageJpaEntity toJpaEntity(ServiceImage image) {
        if (image == null) {
            return null;
        }
        
        return ServiceImageJpaEntity.builder()
                .id(image.getId())
                .serviceId(image.getServiceId())
                .url(image.getUrl())
                .altText(image.getAltText())
                .isPrimary(image.isPrimary())
                .displayOrder(image.getDisplayOrder())
                .createdAt(image.getCreatedAt())
                .updatedAt(image.getUpdatedAt())
                .build();
    }
}
