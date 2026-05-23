package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.SubscriptionAddOn;
import com.ziyara.backend.domain.repository.SubscriptionAddOnRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.SubscriptionAddOnMapper;
import com.ziyara.backend.infrastructure.persistence.repository.SubscriptionAddOnJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SubscriptionAddOnRepositoryAdapter implements SubscriptionAddOnRepository {

    private final SubscriptionAddOnJpaRepository jpaRepository;
    private final SubscriptionAddOnMapper mapper;

    @Override
    public SubscriptionAddOn save(SubscriptionAddOn addOn) {
        return mapper.toDomainEntity(jpaRepository.save(mapper.toJpaEntity(addOn)));
    }

    @Override
    public Optional<SubscriptionAddOn> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public List<SubscriptionAddOn> findBySubscriptionId(UUID subscriptionId) {
        return jpaRepository.findBySubscriptionId(subscriptionId).stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<SubscriptionAddOn> findActiveBySubscriptionId(UUID subscriptionId) {
        return jpaRepository.findActiveBySubscriptionId(subscriptionId, Instant.now()).stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }
}
