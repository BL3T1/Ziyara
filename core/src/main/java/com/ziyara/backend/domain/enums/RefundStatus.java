package com.ziyara.backend.domain.enums;

/**
 * Refund Status Enumeration
 * Tracks the status of refund requests
 */
public enum RefundStatus {
    REQUESTED("Requested"),
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    PROCESSED("Processed"),
    FAILED("Failed"),
    CANCELLED("Cancelled");

    private final String displayName;

    RefundStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isFinal() {
        return this == PROCESSED || this == REJECTED || this == CANCELLED;
    }
}
