package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.CustomerSubscription;
import com.ziyara.backend.domain.enums.SubscriptionStatus;
import com.ziyara.backend.domain.repository.CustomerSubscriptionRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.CustomerSubscriptionMapper;
import com.ziyara.backend.infrastructure.persistence.repository.CustomerSubscriptionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CustomerSubscriptionRepositoryAdapter implements CustomerSubscriptionRepository {

    private final CustomerSubscriptionJpaRepository jpaRepository;
    private final CustomerSubscriptionMapper mapper;

    @Override
    public CustomerSubscription save(CustomerSubscription subscription) {
        return mapper.toDomainEntity(jpaRepository.save(mapper.toJpaEntity(subscription)));
    }

    @Override
    public Optional<CustomerSubscription> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public Optional<CustomerSubscription> findActiveByProviderId(UUID providerId) {
        return jpaRepository.findActiveByProviderId(providerId).map(mapper::toDomainEntity);
    }

    @Override
    public List<CustomerSubscription> findAllByProviderId(UUID providerId) {
        return jpaRepository.findAllByProviderId(providerId).stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerSubscription> findAllByStatus(SubscriptionStatus status) {
        return jpaRepository.findAllByStatus(status).stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }
}
