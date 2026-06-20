package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.ProviderStaff;
import com.ziyara.backend.infrastructure.persistence.entity.ProviderStaffJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ProviderStaffMapper {

    public ProviderStaff toDomainEntity(ProviderStaffJpaEntity entity) {
        if (entity == null) return null;
        ProviderStaff staff = new ProviderStaff();
        staff.setId(entity.getId());
        staff.setProviderId(entity.getProviderId());
        staff.setUserId(entity.getUserId());
        staff.setTitle(entity.getTitle());
        staff.setProviderRole(entity.getProviderRole());
        staff.setCreatedAt(entity.getCreatedAt());
        return staff;
    }

    public ProviderStaffJpaEntity toJpaEntity(ProviderStaff staff) {
        if (staff == null) return null;
        return ProviderStaffJpaEntity.builder()
                .id(staff.getId())
                .providerId(staff.getProviderId())
                .userId(staff.getUserId())
                .title(staff.getTitle())
                .providerRole(staff.getProviderRole())
                .createdAt(staff.getCreatedAt())
                .build();
    }
}
