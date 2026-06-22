package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.SecurityEvent;
import com.ziyara.backend.infrastructure.persistence.entity.SecurityEventJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class SecurityEventMapper {

    public SecurityEvent toDomainEntity(SecurityEventJpaEntity entity) {
        if (entity == null) return null;
        SecurityEvent event = new SecurityEvent();
        event.setId(entity.getId());
        event.setUserId(entity.getUserId());
        event.setEventType(entity.getEventType());
        event.setSeverity(entity.getSeverity());
        event.setIpAddress(entity.getIpAddress());
        event.setUserAgent(entity.getUserAgent());
        event.setDetails(entity.getDetails());
        event.setCreatedAt(entity.getCreatedAt());
        return event;
    }

    public SecurityEventJpaEntity toJpaEntity(SecurityEvent event) {
        if (event == null) return null;
        return SecurityEventJpaEntity.builder()
                .id(event.getId())
                .userId(event.getUserId())
                .eventType(event.getEventType())
                .severity(event.getSeverity())
                .ipAddress(event.getIpAddress())
                .userAgent(event.getUserAgent())
                .details(event.getDetails())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
