package com.ziyara.backend.domain.enums;

/**
 * Complaint Priority Enumeration
 * Defines urgency levels for customer complaints
 */
public enum ComplaintPriority {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    URGENT("Urgent"),
    CRITICAL("Critical");

    private final String displayName;

    ComplaintPriority(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
