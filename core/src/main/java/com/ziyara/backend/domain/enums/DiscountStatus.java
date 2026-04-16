package com.ziyara.backend.domain.enums;

/**
 * Discount Status Enumeration
 * Defines states for discount codes
 */
public enum DiscountStatus {
    PENDING_APPROVAL("Pending approval"),
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    EXPIRED("Expired"),
    FULLY_REDEEMED("Fully Redeemed"),
    SCHEDULED("Scheduled");

    private final String displayName;

    DiscountStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isUsable() {
        return this == ACTIVE;
    }
}
