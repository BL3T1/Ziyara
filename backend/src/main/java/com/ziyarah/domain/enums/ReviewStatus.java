package com.ziyarah.domain.enums;

/**
 * Review Status Enumeration
 * Defines states for customer reviews
 */
public enum ReviewStatus {
    PENDING("Pending Moderation"),
    PUBLISHED("Published"),
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
