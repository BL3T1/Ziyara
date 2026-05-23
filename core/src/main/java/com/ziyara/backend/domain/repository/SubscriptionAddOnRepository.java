package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.SubscriptionAddOn;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionAddOnRepository {
    SubscriptionAddOn save(SubscriptionAddOn addOn);
    Optional<SubscriptionAddOn> findById(UUID id);
    List<SubscriptionAddOn> findBySubscriptionId(UUID subscriptionId);
    /** Active add-ons only (status = ACTIVE and not expired). */
    List<SubscriptionAddOn> findActiveBySubscriptionId(UUID subscriptionId);
}
