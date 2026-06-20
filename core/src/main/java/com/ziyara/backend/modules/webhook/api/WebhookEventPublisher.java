package com.ziyara.backend.modules.webhook.api;

import java.util.Map;

/**
 * Module API for outbound webhook event dispatch.
 * Callers in application.service.* inject this interface — not the concrete WebhookService.
 */
public interface WebhookEventPublisher {

    /**
     * Enqueue a webhook event to be dispatched after the current transaction commits.
     * If called outside a transaction, dispatches immediately.
     *
     * @param event   dot-notation event name, e.g. "booking.created"
     * @param payload key/value data to include in the event body
     */
    void publishAfterCommit(String event, Map<String, Object> payload);
}
