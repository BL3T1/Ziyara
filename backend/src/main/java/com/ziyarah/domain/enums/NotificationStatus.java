package com.ziyarah.domain.enums;

/**
 * Notification Status Enumeration
 * Tracks the delivery and read status of notifications
 */
public enum NotificationStatus {
    PENDING("Pending"),
    SENT("Sent"),
    DELIVERED("Delivered"),
    READ("Read"),
    FAILED("Failed"),
    DISMISSED("Dismissed");

    private final String displayName;

    NotificationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
