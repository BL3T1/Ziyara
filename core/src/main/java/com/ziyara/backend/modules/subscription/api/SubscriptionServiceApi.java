package com.ziyara.backend.modules.subscription.api;

import com.ziyara.backend.application.dto.response.CustomerSubscriptionResponse;

import java.util.UUID;

/**
 * Subscription module API.
 * Consumers (portal, staff management) must depend only on this interface.
 */
public interface SubscriptionServiceApi {

    void assertCanAddUser(UUID providerId);

    int resolveEffectiveSeatLimit(UUID providerId);

    CustomerSubscriptionResponse getSubscription(UUID providerId);
}
