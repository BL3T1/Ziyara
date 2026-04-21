package com.ziyara.backend.application.service;

import com.ziyara.backend.infrastructure.persistence.entity.SecurityEventJpaEntity;
import com.ziyara.backend.infrastructure.persistence.repository.SecurityEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecurityEventService {

    private final SecurityEventJpaRepository securityEventJpaRepository;

    @Transactional
    public void record(UUID userId, String eventType, String severity, String ip, String userAgent, Map<String, Object> details) {
        Map<String, Object> payload = details == null ? null : new HashMap<>(details);
        SecurityEventJpaEntity e = SecurityEventJpaEntity.builder()
                .userId(userId)
                .eventType(eventType)
                .severity(severity)
                .ipAddress(ip)
                .userAgent(userAgent)
                .details(payload == null || payload.isEmpty() ? null : payload)
                .build();
        securityEventJpaRepository.save(e);
    }
}
