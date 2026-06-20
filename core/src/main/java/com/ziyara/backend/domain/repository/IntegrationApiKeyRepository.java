package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.IntegrationApiKey;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IntegrationApiKeyRepository {

    IntegrationApiKey save(IntegrationApiKey key);

    Optional<IntegrationApiKey> findById(UUID id);

    Optional<IntegrationApiKey> findByKeyPrefix(String keyPrefix);

    List<IntegrationApiKey> findAllActive();

    void deleteById(UUID id);
}
