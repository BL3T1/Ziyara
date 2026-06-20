package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.SecurityAlert;
import com.ziyara.backend.domain.repository.SecurityAlertRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.SecurityAlertMapper;
import com.ziyara.backend.infrastructure.persistence.repository.SecurityAlertJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityAlertRepositoryAdapter implements SecurityAlertRepository {

    private final SecurityAlertJpaRepository jpaRepository;
    private final SecurityAlertMapper mapper;

    @Override
    public SecurityAlert save(SecurityAlert alert) {
        return mapper.toDomainEntity(jpaRepository.save(mapper.toJpaEntity(alert)));
    }
}
