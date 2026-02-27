package com.ziyarah.domain.enums;

/**
 * User Status Enumeration
 * Defines all possible user account statuses
 */
public enum UserStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    FROZEN("Frozen"),
    PENDING_VERIFICATION("Pending Verification"),
    DELETED("Deleted");

    private final String displayName;

    UserStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canLogin() {
        return this == ACTIVE;
    }
}
