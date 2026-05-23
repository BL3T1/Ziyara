package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.SystemSetting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SystemSettingRepository {

    SystemSetting save(SystemSetting setting);

    Optional<SystemSetting> findById(UUID id);

    Optional<SystemSetting> findByKey(String settingKey);

    List<SystemSetting> findAll();

    void deleteById(UUID id);
}
