package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.SubscriptionAddOn;
import com.ziyara.backend.infrastructure.persistence.entity.SubscriptionAddOnJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionAddOnMapper {

    public SubscriptionAddOn toDomainEntity(SubscriptionAddOnJpaEntity e) {
        if (e == null) return null;
        SubscriptionAddOn a = new SubscriptionAddOn();
        a.setId(e.getId());
        a.setSubscriptionId(e.getSubscriptionId());
        a.setAddOnCode(e.getAddOnCode());
        a.setDisplayName(e.getDisplayName());
        a.setExtraSeats(e.getExtraSeats());
        a.setPrice(e.getPrice());
        a.setStatus(e.getStatus());
        a.setActivatedAt(e.getActivatedAt());
        a.setExpiresAt(e.getExpiresAt());
        a.setCreatedAt(e.getCreatedAt());
        return a;
    }

    public SubscriptionAddOnJpaEntity toJpaEntity(SubscriptionAddOn a) {
        if (a == null) return null;
        return SubscriptionAddOnJpaEntity.builder()
                .id(a.getId())
                .subscriptionId(a.getSubscriptionId())
                .addOnCode(a.getAddOnCode())
                .displayName(a.getDisplayName())
                .extraSeats(a.getExtraSeats())
                .price(a.getPrice())
                .status(a.getStatus())
                .activatedAt(a.getActivatedAt())
                .expiresAt(a.getExpiresAt())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
