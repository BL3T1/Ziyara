package com.ziyara.backend.infrastructure.job;

import com.ziyara.backend.modules.webhook.api.WebhookRetryApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Retries FAILED webhook deliveries every 5 minutes.
 * A delivery is retried up to 3 total attempts; after that it is marked PERMANENTLY_FAILED.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookRetryJob {

    private final WebhookRetryApi webhookRetryApi;

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void retryFailedDeliveries() {
        int retried = webhookRetryApi.retryFailedDeliveries();
        if (retried > 0) {
            log.info("WebhookRetryJob: retried {} delivery/deliveries", retried);
        }
    }
}
