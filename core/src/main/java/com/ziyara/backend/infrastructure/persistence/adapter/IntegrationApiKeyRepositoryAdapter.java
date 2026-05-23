package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.IntegrationApiKey;
import com.ziyara.backend.domain.repository.IntegrationApiKeyRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.IntegrationApiKeyMapper;
import com.ziyara.backend.infrastructure.persistence.repository.IntegrationApiKeyJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class IntegrationApiKeyRepositoryAdapter implements IntegrationApiKeyRepository {

    private final IntegrationApiKeyJpaRepository jpaRepository;
    private final IntegrationApiKeyMapper mapper;

    @Override
    public IntegrationApiKey save(IntegrationApiKey key) {
        return mapper.toDomainEntity(jpaRepository.save(mapper.toJpaEntity(key)));
    }

    @Override
    public Optional<IntegrationApiKey> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public Optional<IntegrationApiKey> findByKeyPrefix(String keyPrefix) {
        return jpaRepository.findAll().stream()
                .filter(e -> keyPrefix != null && keyPrefix.equals(e.getKeyPrefix()))
                .findFirst()
                .map(mapper::toDomainEntity);
    }

    @Override
    public List<IntegrationApiKey> findAllActive() {
        return jpaRepository.findByRevokedAtIsNullOrderByCreatedAtDesc().stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
