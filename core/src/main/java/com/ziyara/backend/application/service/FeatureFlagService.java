package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.UpsertFeatureFlagRequest;
import com.ziyara.backend.application.dto.response.FeatureFlagResponse;
import com.ziyara.backend.domain.entity.FeatureFlag;
import com.ziyara.backend.domain.enums.NotificationType;
import com.ziyara.backend.domain.repository.FeatureFlagRepository;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationCommandPublisher;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationEvent;
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

    private final FeatureFlagRepository repository;
    private final AuditLogService auditLogService;
    private final StaffNotificationCommandPublisher staffNotificationCommandPublisher;

    @Transactional(readOnly = true)
    public List<FeatureFlagResponse> listAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(FeatureFlag::getFlagKey))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public FeatureFlagResponse upsert(UpsertFeatureFlagRequest request, UUID actorId) {
        String key = request.getFlagKey().trim();
        Instant now = Instant.now();
        FeatureFlag flag = repository.findByKey(key).orElseGet(() -> {
            FeatureFlag f = new FeatureFlag();
            f.setFlagKey(key);
            f.setEnabled(false);
            return f;
        });
        if (request.getEnabled() != null) {
            flag.setEnabled(request.getEnabled());
        }
        if (request.getDescription() != null) {
            String d = request.getDescription().trim();
            flag.setDescription(d.isEmpty() ? null : d);
        }
        flag.setUpdatedAt(now);
        flag.setUpdatedBy(actorId);
        FeatureFlag saved = repository.save(flag);
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
        staffNotificationCommandPublisher.publishAfterCommit(StaffNotificationEvent.builder()
                .eventId(UUID.randomUUID())
                .notificationType(NotificationType.SYSTEM_ALERT.name())
                .title("Feature flag updated")
                .message("Flag " + key + " is now enabled=" + saved.isEnabled())
                .notifyRoles(List.of("CEO", "GENERAL_MANAGER", "SUPER_ADMIN"))
                .metadata("{\"flagKey\":\"" + key + "\"}")
                .build());
        return toResponse(saved);
    }

    private FeatureFlagResponse toResponse(FeatureFlag f) {
        return FeatureFlagResponse.builder()
                .id(f.getId())
                .flagKey(f.getFlagKey())
                .enabled(f.isEnabled())
                .description(f.getDescription())
                .updatedAt(f.getUpdatedAt())
                .updatedBy(f.getUpdatedBy())
                .build();
    }
}
