package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.SecurityEvent;
import com.ziyara.backend.domain.repository.SecurityEventRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.SecurityEventMapper;
import com.ziyara.backend.infrastructure.persistence.repository.SecurityEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class SecurityEventRepositoryAdapter implements SecurityEventRepository {

    private final SecurityEventJpaRepository jpaRepository;
    private final SecurityEventMapper mapper;

    @Override
    public SecurityEvent save(SecurityEvent event) {
        return mapper.toDomainEntity(jpaRepository.save(mapper.toJpaEntity(event)));
    }

    @Override
    public long countByTypeAndIpSince(String eventType, String ipAddress, Instant since) {
        return jpaRepository.countByTypeAndIpSince(eventType, ipAddress, since);
    }
}
