package com.ziyarah.domain.enums;

/**
 * Booking Status Enumeration
 * Defines all possible states in the booking lifecycle
 */
public enum BookingStatus {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    ACTIVE("Active"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    EXPIRED("Expired"),
    REFUNDING("Refunding"),
    REFUNDED("Refunded"),
    REFUND_FAILED("Refund Failed"),
    MANUAL_REVIEW("Manual Review"),
    REVIEW_PENDING("Review Pending"),
    REVIEWED("Reviewed"),
    CLOSED("Closed");

    private final String displayName;

    BookingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canBeCancelled() {
        return this == PENDING || this == CONFIRMED || this == ACTIVE;
    }

    public boolean canBeModified() {
        return this == PENDING || this == CONFIRMED;
    }

    public boolean requiresRefund() {
        return this == CANCELLED && (this != REFUNDED && this != REFUND_FAILED);
    }

    public boolean isFinalState() {
        return this == COMPLETED || this == REFUNDED || this == CLOSED || this == EXPIRED;
    }

    public boolean canAcceptReview() {
        return this == COMPLETED || this == REVIEW_PENDING;
    }
}
