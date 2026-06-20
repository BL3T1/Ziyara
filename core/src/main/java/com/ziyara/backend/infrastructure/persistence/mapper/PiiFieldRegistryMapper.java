package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.PiiFieldRegistry;
import com.ziyara.backend.infrastructure.persistence.entity.PiiFieldRegistryJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PiiFieldRegistryMapper {

    public PiiFieldRegistry toDomainEntity(PiiFieldRegistryJpaEntity entity) {
        if (entity == null) return null;
        PiiFieldRegistry registry = new PiiFieldRegistry();
        registry.setId(entity.getId());
        registry.setTableName(entity.getTableName());
        registry.setColumnName(entity.getColumnName());
        registry.setPiiCategory(entity.getPiiCategory());
        registry.setEncryptionRequired(entity.getEncryptionRequired());
        registry.setGdprArticle(entity.getGdprArticle());
        registry.setLastReviewedAt(entity.getLastReviewedAt());
        return registry;
    }

    public PiiFieldRegistryJpaEntity toJpaEntity(PiiFieldRegistry registry) {
        if (registry == null) return null;
        return PiiFieldRegistryJpaEntity.builder()
                .id(registry.getId())
                .tableName(registry.getTableName())
                .columnName(registry.getColumnName())
                .piiCategory(registry.getPiiCategory())
                .encryptionRequired(registry.getEncryptionRequired())
                .gdprArticle(registry.getGdprArticle())
                .lastReviewedAt(registry.getLastReviewedAt())
                .build();
    }
}
