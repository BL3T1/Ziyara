package com.ziyarah.domain.enums;

/**
 * Provider Status Enumeration
 * Defines states for service providers
 */
public enum ProviderStatus {
    PENDING_APPROVAL("Pending Approval"),
    ACTIVE("Active"),
    SUSPENDED("Suspended"),
    INACTIVE("Inactive"),
    REJECTED("Rejected"),
    BLOCKED("Blocked");

    private final String displayName;

    ProviderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canProvideService() {
        return this == ACTIVE;
    }
}
