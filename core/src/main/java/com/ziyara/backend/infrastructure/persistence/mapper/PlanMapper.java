package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.Plan;
import com.ziyara.backend.infrastructure.persistence.entity.PlanJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PlanMapper {

    public Plan toDomainEntity(PlanJpaEntity e) {
        if (e == null) return null;
        Plan p = new Plan();
        p.setId(e.getId());
        p.setCode(e.getCode());
        p.setName(e.getName());
        p.setDescription(e.getDescription());
        p.setMaxUsers(e.getMaxUsers());
        p.setMonthlyPrice(e.getMonthlyPrice());
        p.setCurrency(e.getCurrency());
        p.setAllowsOverage(e.isAllowsOverage());
        p.setOveragePricePerUser(e.getOveragePricePerUser());
        p.setActive(e.isActive());
        p.setCreatedAt(e.getCreatedAt());
        p.setUpdatedAt(e.getUpdatedAt());
        return p;
    }
}
