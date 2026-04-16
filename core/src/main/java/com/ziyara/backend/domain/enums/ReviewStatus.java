package com.ziyara.backend.domain.enums;

/**
 * Review Status Enumeration
 * Defines states for customer reviews
 */
public enum ReviewStatus {
    PENDING("Pending Moderation"),
    PUBLISHED("Published"),
    APPROVED("Approved"), // Same as PUBLISHED, for DB compatibility
    REJECTED("Rejected"),
    HIDDEN("Hidden"),
    REPORTED("Reported");

    private final String displayName;

    ReviewStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isVisible() {
        return this == PUBLISHED;
    }
}
