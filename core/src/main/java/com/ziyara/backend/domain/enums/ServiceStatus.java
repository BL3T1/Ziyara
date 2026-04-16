package com.ziyara.backend.domain.enums;

/**
 * Service Status Enumeration
 * Defines availability of individual services
 */
public enum ServiceStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    SUSPENDED("Suspended"),
    PENDING_APPROVAL("Pending Approval"),
    AVAILABLE("Available"),
    UNAVAILABLE("Unavailable"),
    MAINTENANCE("Under Maintenance"),
    DISCONTINUED("Discontinued"),
    HIDDEN("Hidden");

    private final String displayName;

    ServiceStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isBookable() {
        return this == AVAILABLE;
    }
}
