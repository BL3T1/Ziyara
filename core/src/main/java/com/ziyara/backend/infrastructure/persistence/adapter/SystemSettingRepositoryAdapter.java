package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.SystemSetting;
import com.ziyara.backend.domain.repository.SystemSettingRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.SystemSettingMapper;
import com.ziyara.backend.infrastructure.persistence.repository.SystemSettingJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SystemSettingRepositoryAdapter implements SystemSettingRepository {

    private final SystemSettingJpaRepository jpaRepository;
    private final SystemSettingMapper mapper;

    @Override
    public SystemSetting save(SystemSetting setting) {
        return mapper.toDomainEntity(jpaRepository.save(mapper.toJpaEntity(setting)));
    }

    @Override
    public Optional<SystemSetting> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public Optional<SystemSetting> findByKey(String settingKey) {
        return jpaRepository.findBySettingKey(settingKey).map(mapper::toDomainEntity);
    }

    @Override
    public List<SystemSetting> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
