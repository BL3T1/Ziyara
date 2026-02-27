package com.ziyarah.domain.enums;

/**
 * Enum for internal ticket status
 */
public enum TicketStatus {
    SUBMITTED("Submitted", true, false),
    ACKNOWLEDGED("Acknowledged", true, false),
    ASSIGNED("Assigned", true, false),
    IN_PROGRESS("In Progress", true, false),
    PENDING_INFO("Pending Information", true, false),
    TESTING("Testing", true, false),
    RESOLVED("Resolved", false, true),
    VERIFIED("Verified", false, true),
    CLOSED("Closed", false, false),
    REOPENED("Reopened", true, false),
    CANCELLED("Cancelled", false, false);

    private final String displayName;
    private final boolean isOpen;
    private final boolean isResolved;

    TicketStatus(String displayName, boolean isOpen, boolean isResolved) {
        this.displayName = displayName;
        this.isOpen = isOpen;
        this.isResolved = isResolved;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public boolean isResolved() {
        return isResolved;
    }

    public boolean canBeAssigned() {
        return this == SUBMITTED || this == ACKNOWLEDGED;
    }

    public boolean canBeResolved() {
        return this == IN_PROGRESS || this == TESTING || this == PENDING_INFO;
    }

    public boolean canBeClosed() {
        return this == RESOLVED || this == VERIFIED;
    }

    public boolean canBeReopened() {
        return this == CLOSED || this == CANCELLED;
    }

    public boolean canBeCancelled() {
        return this == SUBMITTED || this == ACKNOWLEDGED || this == ASSIGNED;
    }
}
