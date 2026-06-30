package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.WebhookDelivery;
import com.ziyara.backend.domain.entity.WebhookRetryTask;

import java.util.List;
import java.util.UUID;

public interface WebhookDeliveryRepository {

    void insert(WebhookDelivery delivery);

    void update(WebhookDelivery delivery);

    List<WebhookRetryTask> findFailedForRetry(int limit);

    List<WebhookDelivery> findBySubscriptionId(UUID subscriptionId, int limit, long offset);
}
