package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.FeatureFlag;
import com.ziyara.backend.domain.repository.FeatureFlagRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.FeatureFlagMapper;
import com.ziyara.backend.infrastructure.persistence.repository.FeatureFlagJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FeatureFlagRepositoryAdapter implements FeatureFlagRepository {

    private final FeatureFlagJpaRepository jpaRepository;
    private final FeatureFlagMapper mapper;

    @Override
    public FeatureFlag save(FeatureFlag featureFlag) {
        return mapper.toDomainEntity(jpaRepository.save(mapper.toJpaEntity(featureFlag)));
    }

    @Override
    public Optional<FeatureFlag> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public Optional<FeatureFlag> findByKey(String flagKey) {
        return jpaRepository.findByFlagKey(flagKey).map(mapper::toDomainEntity);
    }

    @Override
    public List<FeatureFlag> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
