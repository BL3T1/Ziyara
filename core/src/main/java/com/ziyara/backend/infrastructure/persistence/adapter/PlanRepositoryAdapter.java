package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.Plan;
import com.ziyara.backend.domain.repository.PlanRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.PlanMapper;
import com.ziyara.backend.infrastructure.persistence.repository.PlanJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PlanRepositoryAdapter implements PlanRepository {

    private final PlanJpaRepository jpaRepository;
    private final PlanMapper mapper;

    @Override
    public List<Plan> findAllActive() {
        return jpaRepository.findByActiveTrue().stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Plan> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public Optional<Plan> findByCode(String code) {
        return jpaRepository.findByCode(code).map(mapper::toDomainEntity);
    }
}
