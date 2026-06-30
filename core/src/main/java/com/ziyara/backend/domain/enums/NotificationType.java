package com.ziyara.backend.domain.enums;

/**
 * Notification Type Enumeration
 * Categorizes notifications by their purpose
 */
public enum NotificationType {
    BOOKING_CONFIRMATION("Booking Confirmation"),
    BOOKING_CANCELLED("Booking Cancelled"),
    PAYMENT_SUCCESS("Payment Success"),
    PAYMENT_FAILED("Payment Failed"),
    SYSTEM_ALERT("System Alert"),
    PROMOTIONAL("Promotional"),
    /** Provider submitted and awaiting company approval. */
    PROVIDER_PENDING_REVIEW("Provider Pending Review"),
    /** Company staff account was created (HR visibility). */
    STAFF_USER_CREATED("Staff User Created"),
    /** Booking moved to confirmed (ops visibility). */
    BOOKING_CONFIRMED_STAFF("Booking Confirmed (Staff)"),
    /** Provider submitted media (image) awaiting admin review. */
    MEDIA_SUBMISSION_PENDING("Media Submission Pending"),
    /** Partner account will expire within 7 days — admin action needed. */
    PROVIDER_EXPIRY_WARNING("Partner Expiry Warning"),
    /** Partner account has expired today — login blocked until renewed. */
    PROVIDER_EXPIRED("Partner Account Expired");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
