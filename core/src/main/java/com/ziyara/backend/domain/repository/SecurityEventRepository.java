package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.SecurityEvent;

import java.time.Instant;

public interface SecurityEventRepository {

    SecurityEvent save(SecurityEvent event);

    long countByTypeAndIpSince(String eventType, String ipAddress, Instant since);
}
