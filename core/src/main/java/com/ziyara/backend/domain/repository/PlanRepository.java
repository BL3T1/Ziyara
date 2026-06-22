package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.Plan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository {
    List<Plan> findAllActive();
    Optional<Plan> findById(UUID id);
    Optional<Plan> findByCode(String code);
}
