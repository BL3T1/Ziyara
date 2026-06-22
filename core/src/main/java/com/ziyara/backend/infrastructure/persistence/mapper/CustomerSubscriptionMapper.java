package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.CustomerSubscription;
import com.ziyara.backend.infrastructure.persistence.entity.CustomerSubscriptionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CustomerSubscriptionMapper {

    public CustomerSubscription toDomainEntity(CustomerSubscriptionJpaEntity e) {
        if (e == null) return null;
        CustomerSubscription s = new CustomerSubscription();
        s.setId(e.getId());
        s.setProviderId(e.getProviderId());
        s.setPlanId(e.getPlanId());
        s.setStatus(e.getStatus());
        s.setSeatLimit(e.getSeatLimit());
        s.setCurrentPeriodStart(e.getCurrentPeriodStart());
        s.setCurrentPeriodEnd(e.getCurrentPeriodEnd());
        s.setTrialEndsAt(e.getTrialEndsAt());
        s.setCancelledAt(e.getCancelledAt());
        s.setCreatedAt(e.getCreatedAt());
        s.setUpdatedAt(e.getUpdatedAt());
        return s;
    }

    public CustomerSubscriptionJpaEntity toJpaEntity(CustomerSubscription s) {
        if (s == null) return null;
        return CustomerSubscriptionJpaEntity.builder()
                .id(s.getId())
                .providerId(s.getProviderId())
                .planId(s.getPlanId())
                .status(s.getStatus())
                .seatLimit(s.getSeatLimit())
                .currentPeriodStart(s.getCurrentPeriodStart())
                .currentPeriodEnd(s.getCurrentPeriodEnd())
                .trialEndsAt(s.getTrialEndsAt())
                .cancelledAt(s.getCancelledAt())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
