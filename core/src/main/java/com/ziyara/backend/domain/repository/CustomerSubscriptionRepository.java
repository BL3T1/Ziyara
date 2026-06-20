package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.CustomerSubscription;
import com.ziyara.backend.domain.enums.SubscriptionStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerSubscriptionRepository {
    CustomerSubscription save(CustomerSubscription subscription);
    Optional<CustomerSubscription> findById(UUID id);
    /** The currently usable (TRIAL or ACTIVE) subscription for this provider. */
    Optional<CustomerSubscription> findActiveByProviderId(UUID providerId);
    List<CustomerSubscription> findAllByProviderId(UUID providerId);
    List<CustomerSubscription> findAllByStatus(SubscriptionStatus status);
}
