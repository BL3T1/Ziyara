package com.ziyarah.infrastructure.persistence.mapper;

import com.ziyarah.domain.entity.DiscountCode;
import com.ziyarah.infrastructure.persistence.entity.DiscountCodeJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper: DiscountCodeMapper
 */
@Component
public class DiscountCodeMapper {
    
    public DiscountCode toDomainEntity(DiscountCodeJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        DiscountCode discountCode = new DiscountCode();
        discountCode.setId(entity.getId());
        discountCode.setCode(entity.getCode());
        discountCode.setDescription(entity.getDescription());
        discountCode.setType(entity.getType());
        discountCode.setValue(entity.getValue());
        discountCode.setMinBookingAmount(entity.getMinBookingAmount());
        discountCode.setMaxDiscountAmount(entity.getMaxDiscountAmount());
        discountCode.setStartDate(entity.getStartDate());
        discountCode.setEndDate(entity.getEndDate());
        discountCode.setUsageLimit(entity.getUsageLimit() != null ? entity.getUsageLimit() : 0);
        discountCode.setUsageCount(entity.getUsageCount() != null ? entity.getUsageCount() : 0);
        discountCode.setStatus(entity.getStatus());
        discountCode.setCreatedAt(entity.getCreatedAt());
        discountCode.setUpdatedAt(entity.getUpdatedAt());
        
        return discountCode;
    }
    
    public DiscountCodeJpaEntity toJpaEntity(DiscountCode discountCode) {
        if (discountCode == null) {
            return null;
        }
        
        return DiscountCodeJpaEntity.builder()
                .id(discountCode.getId())
                .code(discountCode.getCode())
                .description(discountCode.getDescription())
                .type(discountCode.getType())
                .value(discountCode.getValue())
                .minBookingAmount(discountCode.getMinBookingAmount())
                .maxDiscountAmount(discountCode.getMaxDiscountAmount())
                .startDate(discountCode.getStartDate())
                .endDate(discountCode.getEndDate())
                .usageLimit(discountCode.getUsageLimit())
                .usageCount(discountCode.getUsageCount())
                .status(discountCode.getStatus())
                .createdAt(discountCode.getCreatedAt())
                .updatedAt(discountCode.getUpdatedAt())
                .build();
    }
}
