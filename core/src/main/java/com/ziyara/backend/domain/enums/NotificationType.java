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
    TICKET_CREATED("Ticket Created"),
    TICKET_RESOLVED("Ticket Resolved"),
    SYSTEM_ALERT("System Alert"),
    PROMOTIONAL("Promotional"),
    COMPLAINT_UPDATE("Complaint Update"),
    /** New complaint ticket (staff distribution). */
    COMPLAINT_NEW("New Complaint"),
    /** Provider submitted and awaiting company approval. */
    PROVIDER_PENDING_REVIEW("Provider Pending Review"),
    /** Company staff account was created (HR visibility). */
    STAFF_USER_CREATED("Staff User Created"),
    /** Booking moved to confirmed (ops visibility). */
    BOOKING_CONFIRMED_STAFF("Booking Confirmed (Staff)"),
    /** Provider portal support request submitted to Ziyara staff. */
    PORTAL_SUPPORT_REQUEST("Provider Support Request"),
    /** Provider submitted media (image) awaiting admin review. */
    MEDIA_SUBMISSION_PENDING("Media Submission Pending");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
