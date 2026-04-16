package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.SystemSettingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SystemSettingJpaRepository extends JpaRepository<SystemSettingJpaEntity, UUID> {
    Optional<SystemSettingJpaEntity> findBySettingKey(String settingKey);
}
