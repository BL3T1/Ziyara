package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.SecurityAlertRule;
import com.ziyara.backend.domain.repository.SecurityAlertRuleRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.SecurityAlertRuleMapper;
import com.ziyara.backend.infrastructure.persistence.repository.SecurityAlertRuleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SecurityAlertRuleRepositoryAdapter implements SecurityAlertRuleRepository {

    private final SecurityAlertRuleJpaRepository jpaRepository;
    private final SecurityAlertRuleMapper mapper;

    @Override
    public List<SecurityAlertRule> findByEventTypeEnabled(String eventType) {
        return jpaRepository.findByEventTypeAndEnabledTrue(eventType).stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SecurityAlertRule> findFirstByName(String name) {
        return jpaRepository.findFirstByName(name).map(mapper::toDomainEntity);
    }
}
