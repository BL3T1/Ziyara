package com.ziyara.backend.application.service;

import com.ziyara.backend.domain.entity.SecurityEvent;
import com.ziyara.backend.domain.repository.SecurityEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecurityEventService {

    private final SecurityEventRepository securityEventRepository;

    @Transactional
    public void record(UUID userId, String eventType, String severity, String ip, String userAgent, Map<String, Object> details) {
        Map<String, Object> payload = details == null ? null : new HashMap<>(details);
        SecurityEvent event = new SecurityEvent();
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setSeverity(severity);
        event.setIpAddress(ip);
        event.setUserAgent(userAgent);
        event.setDetails(payload == null || payload.isEmpty() ? null : payload);
        securityEventRepository.save(event);
    }
}
