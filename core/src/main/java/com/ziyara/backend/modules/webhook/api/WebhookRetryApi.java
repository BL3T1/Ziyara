package com.ziyara.backend.modules.webhook.api;

/** Module API for triggering failed delivery retries from infrastructure jobs. */
public interface WebhookRetryApi {
    /** Retries FAILED deliveries (up to 3 total attempts). Returns the number retried. */
    int retryFailedDeliveries();
}
