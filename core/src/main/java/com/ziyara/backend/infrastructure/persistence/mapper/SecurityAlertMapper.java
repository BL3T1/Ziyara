package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.SecurityAlert;
import com.ziyara.backend.infrastructure.persistence.entity.SecurityAlertJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class SecurityAlertMapper {

    public SecurityAlert toDomainEntity(SecurityAlertJpaEntity entity) {
        if (entity == null) return null;
        SecurityAlert alert = new SecurityAlert();
        alert.setId(entity.getId());
        alert.setRuleId(entity.getRuleId());
        alert.setUserId(entity.getUserId());
        alert.setTriggeredBy(entity.getTriggeredBy());
        alert.setOccurrenceCount(entity.getOccurrenceCount());
        alert.setSeverity(entity.getSeverity());
        alert.setStatus(entity.getStatus());
        alert.setCreatedAt(entity.getCreatedAt());
        return alert;
    }

    public SecurityAlertJpaEntity toJpaEntity(SecurityAlert alert) {
        if (alert == null) return null;
        return SecurityAlertJpaEntity.builder()
                .id(alert.getId())
                .ruleId(alert.getRuleId())
                .userId(alert.getUserId())
                .triggeredBy(alert.getTriggeredBy())
                .occurrenceCount(alert.getOccurrenceCount())
                .severity(alert.getSeverity())
                .status(alert.getStatus())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}
