package com.ziyara.backend.modules.provider.api;

/**
 * Module API for the provider expiry check.
 * Infrastructure scheduled jobs must depend only on this interface.
 */
public interface ProviderExpiryApi {

    /** Check for providers expiring today or in 7 days and notify admins. */
    void notifyExpiringProviders();
}
