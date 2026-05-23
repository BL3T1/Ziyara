package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.IntegrationApiKey;
import com.ziyara.backend.infrastructure.persistence.entity.IntegrationApiKeyJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class IntegrationApiKeyMapper {

    public IntegrationApiKey toDomainEntity(IntegrationApiKeyJpaEntity entity) {
        if (entity == null) return null;
        IntegrationApiKey key = new IntegrationApiKey();
        key.setId(entity.getId());
        key.setName(entity.getName());
        key.setKeyPrefix(entity.getKeyPrefix());
        key.setSecretHash(entity.getSecretHash());
        key.setCreatedAt(entity.getCreatedAt());
        key.setRevokedAt(entity.getRevokedAt());
        key.setLastUsedAt(entity.getLastUsedAt());
        return key;
    }

    public IntegrationApiKeyJpaEntity toJpaEntity(IntegrationApiKey key) {
        if (key == null) return null;
        return IntegrationApiKeyJpaEntity.builder()
                .id(key.getId())
                .name(key.getName())
                .keyPrefix(key.getKeyPrefix())
                .secretHash(key.getSecretHash())
                .createdAt(key.getCreatedAt())
                .revokedAt(key.getRevokedAt())
                .lastUsedAt(key.getLastUsedAt())
                .build();
    }
}
