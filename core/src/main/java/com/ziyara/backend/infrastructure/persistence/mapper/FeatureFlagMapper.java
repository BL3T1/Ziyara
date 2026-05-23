package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.FeatureFlag;
import com.ziyara.backend.infrastructure.persistence.entity.FeatureFlagJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class FeatureFlagMapper {

    public FeatureFlag toDomainEntity(FeatureFlagJpaEntity entity) {
        if (entity == null) return null;
        FeatureFlag flag = new FeatureFlag();
        flag.setId(entity.getId());
        flag.setFlagKey(entity.getFlagKey());
        flag.setEnabled(entity.isEnabled());
        flag.setDescription(entity.getDescription());
        flag.setUpdatedAt(entity.getUpdatedAt());
        flag.setUpdatedBy(entity.getUpdatedBy());
        return flag;
    }

    public FeatureFlagJpaEntity toJpaEntity(FeatureFlag flag) {
        if (flag == null) return null;
        return FeatureFlagJpaEntity.builder()
                .id(flag.getId())
                .flagKey(flag.getFlagKey())
                .enabled(flag.isEnabled())
                .description(flag.getDescription())
                .updatedAt(flag.getUpdatedAt())
                .updatedBy(flag.getUpdatedBy())
                .build();
    }
}
