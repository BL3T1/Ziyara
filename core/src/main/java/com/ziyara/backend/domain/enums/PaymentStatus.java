package com.ziyara.backend.domain.enums;

/**
 * Payment Status Enumeration
 */
public enum PaymentStatus {
    PENDING("Pending"),
    PROCESSING("Processing"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    CANCELLED("Cancelled"),
    REFUNDED("Refunded");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isSuccessful() {
        return this == COMPLETED;
    }

    public boolean canBeRefunded() {
        return this == COMPLETED;
    }
}
