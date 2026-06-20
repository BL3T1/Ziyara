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
    REFUNDED("Refunded"),
    COLLECTED("Collected"),
    RECORDED("Recorded"),
    RECONCILED("Reconciled"),
    NO_SHOW_FORFEIT("No-Show Forfeit");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isSuccessful() {
        return this == COMPLETED || this == RECONCILED;
    }

    public boolean canBeRefunded() {
        return this == COMPLETED || this == RECONCILED;
    }

    public boolean isPortalRecorded() {
        return this == COLLECTED || this == RECORDED || this == RECONCILED;
    }
}
