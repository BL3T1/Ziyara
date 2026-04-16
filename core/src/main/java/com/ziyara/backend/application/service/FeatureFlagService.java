package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.UpsertFeatureFlagRequest;
import com.ziyara.backend.application.dto.response.FeatureFlagResponse;
import com.ziyara.backend.infrastructure.persistence.entity.FeatureFlagJpaEntity;
import com.ziyara.backend.infrastructure.persistence.repository.FeatureFlagJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeatureFlagService {

    private final FeatureFlagJpaRepository repository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<FeatureFlagResponse> listAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(FeatureFlagJpaEntity::getFlagKey))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public FeatureFlagResponse upsert(UpsertFeatureFlagRequest request, UUID actorId) {
        String key = request.getFlagKey().trim();
        Instant now = Instant.now();
        FeatureFlagJpaEntity row = repository.findByFlagKey(key).orElseGet(() ->
                FeatureFlagJpaEntity.builder().flagKey(key).enabled(false).build());
        if (request.getEnabled() != null) {
            row.setEnabled(request.getEnabled());
        }
        if (request.getDescription() != null) {
            String d = request.getDescription().trim();
            row.setDescription(d.isEmpty() ? null : d);
        }
        row.setUpdatedAt(now);
        row.setUpdatedBy(actorId);
        FeatureFlagJpaEntity saved = repository.save(row);
        auditLogService.logAction(
                "FEATURE_FLAG_UPSERT",
                "FeatureFlag",
                key,
                actorId,
                null,
                "enabled=" + saved.isEnabled(),
                null,
                null
        );
        return toResponse(saved);
    }

    private FeatureFlagResponse toResponse(FeatureFlagJpaEntity e) {
        return FeatureFlagResponse.builder()
                .id(e.getId())
                .flagKey(e.getFlagKey())
                .enabled(e.isEnabled())
                .description(e.getDescription())
                .updatedAt(e.getUpdatedAt())
                .updatedBy(e.getUpdatedBy())
                .build();
    }
}
