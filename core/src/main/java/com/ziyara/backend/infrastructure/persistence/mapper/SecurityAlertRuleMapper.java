package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.SecurityAlertRule;
import com.ziyara.backend.infrastructure.persistence.entity.SecurityAlertRuleJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class SecurityAlertRuleMapper {

    public SecurityAlertRule toDomainEntity(SecurityAlertRuleJpaEntity entity) {
        if (entity == null) return null;
        SecurityAlertRule rule = new SecurityAlertRule();
        rule.setId(entity.getId());
        rule.setName(entity.getName());
        rule.setDescription(entity.getDescription());
        rule.setEventType(entity.getEventType());
        rule.setThreshold(entity.getThreshold());
        rule.setTimeWindowMinutes(entity.getTimeWindowMinutes());
        rule.setSeverity(entity.getSeverity());
        rule.setEnabled(entity.getEnabled());
        rule.setCooldownMinutes(entity.getCooldownMinutes());
        rule.setCreatedAt(entity.getCreatedAt());
        return rule;
    }

    public SecurityAlertRuleJpaEntity toJpaEntity(SecurityAlertRule rule) {
        if (rule == null) return null;
        return SecurityAlertRuleJpaEntity.builder()
                .id(rule.getId())
                .name(rule.getName())
                .description(rule.getDescription())
                .eventType(rule.getEventType())
                .threshold(rule.getThreshold())
                .timeWindowMinutes(rule.getTimeWindowMinutes())
                .severity(rule.getSeverity())
                .enabled(rule.getEnabled())
                .cooldownMinutes(rule.getCooldownMinutes())
                .createdAt(rule.getCreatedAt())
                .build();
    }
}
