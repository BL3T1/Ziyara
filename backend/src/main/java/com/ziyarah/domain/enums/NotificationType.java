package com.ziyarah.domain.enums;

/**
 * Notification Type Enumeration
 * Categorizes notifications by their purpose
 */
public enum NotificationType {
    BOOKING_CONFIRMATION("Booking Confirmation"),
    BOOKING_CANCELLED("Booking Cancelled"),
    PAYMENT_SUCCESS("Payment Success"),
    PAYMENT_FAILED("Payment Failed"),
    TICKET_CREATED("Ticket Created"),
    TICKET_RESOLVED("Ticket Resolved"),
    SYSTEM_ALERT("System Alert"),
    PROMOTIONAL("Promotional"),
    COMPLAINT_UPDATE("Complaint Update");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
