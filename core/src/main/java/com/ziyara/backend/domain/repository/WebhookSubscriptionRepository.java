package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.WebhookSubscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookSubscriptionRepository {

    WebhookSubscription save(WebhookSubscription subscription);

    Optional<WebhookSubscription> findById(UUID id);

    List<WebhookSubscription> findActiveByEvent(String event);

    List<WebhookSubscription> findAll(int limit, long offset);

    void deleteById(UUID id);

    void setActive(UUID id, boolean active);
}
