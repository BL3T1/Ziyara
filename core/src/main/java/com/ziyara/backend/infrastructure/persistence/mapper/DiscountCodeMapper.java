package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.DiscountCode;
import com.ziyara.backend.infrastructure.persistence.entity.DiscountCodeJpaEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
        discountCode.setCreatedBy(entity.getCreatedBy());
        discountCode.setSponsor(entity.getSponsor());
        discountCode.setProviderId(entity.getProviderId());
        discountCode.setApplicableServiceIds(fromStringUuidList(entity.getApplicableServiceIds()));
        discountCode.setApplicableMenuSectionIds(fromStringUuidList(entity.getApplicableMenuSectionIds()));
        discountCode.setApplicableMenuItemIds(fromStringUuidList(entity.getApplicableMenuItemIds()));
        discountCode.setApplicableRoomTypeIds(fromStringUuidList(entity.getApplicableRoomTypeIds()));
        discountCode.setApprovalStatus(entity.getApprovalStatus());
        discountCode.setApprovedBy(entity.getApprovedBy());
        discountCode.setApprovedAt(entity.getApprovedAt());

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
                .createdBy(discountCode.getCreatedBy())
                .sponsor(discountCode.getSponsor())
                .providerId(discountCode.getProviderId())
                .applicableServiceIds(toStringUuidList(discountCode.getApplicableServiceIds()))
                .applicableMenuSectionIds(toStringUuidList(discountCode.getApplicableMenuSectionIds()))
                .applicableMenuItemIds(toStringUuidList(discountCode.getApplicableMenuItemIds()))
                .applicableRoomTypeIds(toStringUuidList(discountCode.getApplicableRoomTypeIds()))
                .approvalStatus(discountCode.getApprovalStatus())
                .approvedBy(discountCode.getApprovedBy())
                .approvedAt(discountCode.getApprovedAt())
                .build();
    }

    private static List<String> toStringUuidList(List<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return null;
        }
        return uuids.stream().map(UUID::toString).collect(Collectors.toList());
    }

    private static List<UUID> fromStringUuidList(List<String> strings) {
        if (strings == null || strings.isEmpty()) {
            return null;
        }
        return strings.stream().map(UUID::fromString).collect(Collectors.toList());
    }
}
