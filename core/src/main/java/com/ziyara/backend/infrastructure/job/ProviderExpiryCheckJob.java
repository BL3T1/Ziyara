package com.ziyara.backend.infrastructure.job;

import com.ziyara.backend.modules.provider.api.ProviderExpiryApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily job: checks for expiring/expired partner accounts and notifies admins.
 * Runs at 08:00 UTC every day.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProviderExpiryCheckJob {

    private final ProviderExpiryApi providerExpiryApi;

    @Scheduled(cron = "${ziyara.provider.expiry.cron:0 0 8 * * *}", zone = "UTC")
    public void run() {
        log.info("[ProviderExpiryCheckJob] Starting daily expiry check");
        try {
            providerExpiryApi.notifyExpiringProviders();
        } catch (Exception ex) {
            log.error("[ProviderExpiryCheckJob] Expiry check failed: {}", ex.getMessage(), ex);
        }
    }
}
