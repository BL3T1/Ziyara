package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.ProviderSubscription;
import com.ziyara.backend.domain.repository.ProviderSubscriptionRepository;
import com.ziyara.backend.infrastructure.persistence.entity.ProviderSubscriptionJpaEntity;
import com.ziyara.backend.infrastructure.persistence.repository.ProviderSubscriptionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProviderSubscriptionRepositoryAdapter implements ProviderSubscriptionRepository {

    private final ProviderSubscriptionJpaRepository jpaRepository;

    @Override
    public Optional<ProviderSubscription> findByProviderId(UUID providerId) {
        return jpaRepository.findByProviderId(providerId).map(this::toDomain);
    }

    @Override
    public List<ProviderSubscription> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public ProviderSubscription save(ProviderSubscription sub) {
        return toDomain(jpaRepository.save(toJpa(sub)));
    }

    private ProviderSubscription toDomain(ProviderSubscriptionJpaEntity e) {
        ProviderSubscription s = new ProviderSubscription();
        s.setId(e.getId());
        s.setProviderId(e.getProviderId());
        s.setPlan(e.getPlan());
        s.setStaffLimit(e.getStaffLimit());
        s.setCreatedAt(e.getCreatedAt());
        s.setUpdatedAt(e.getUpdatedAt());
        return s;
    }

    private ProviderSubscriptionJpaEntity toJpa(ProviderSubscription s) {
        return ProviderSubscriptionJpaEntity.builder()
                .id(s.getId())
                .providerId(s.getProviderId())
                .plan(s.getPlan())
                .staffLimit(s.getStaffLimit())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
