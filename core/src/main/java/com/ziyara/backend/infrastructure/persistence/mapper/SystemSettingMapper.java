package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.SystemSetting;
import com.ziyara.backend.infrastructure.persistence.entity.SystemSettingJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class SystemSettingMapper {

    public SystemSetting toDomainEntity(SystemSettingJpaEntity entity) {
        if (entity == null) return null;
        SystemSetting setting = new SystemSetting();
        setting.setId(entity.getId());
        setting.setSettingKey(entity.getSettingKey());
        setting.setValueJson(entity.getValueJson());
        setting.setUpdatedAt(entity.getUpdatedAt());
        setting.setUpdatedBy(entity.getUpdatedBy());
        return setting;
    }

    public SystemSettingJpaEntity toJpaEntity(SystemSetting setting) {
        if (setting == null) return null;
        return SystemSettingJpaEntity.builder()
                .id(setting.getId())
                .settingKey(setting.getSettingKey())
                .valueJson(setting.getValueJson())
                .updatedAt(setting.getUpdatedAt())
                .updatedBy(setting.getUpdatedBy())
                .build();
    }
}
