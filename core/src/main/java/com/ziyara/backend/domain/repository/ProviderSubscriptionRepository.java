package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.ProviderSubscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderSubscriptionRepository {
    Optional<ProviderSubscription> findByProviderId(UUID providerId);
    List<ProviderSubscription> findAll();
    ProviderSubscription save(ProviderSubscription subscription);
}
