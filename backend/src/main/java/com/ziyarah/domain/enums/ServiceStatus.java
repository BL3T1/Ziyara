package com.ziyarah.domain.enums;

/**
 * Service Status Enumeration
 * Defines availability of individual services
 */
public enum ServiceStatus {
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
