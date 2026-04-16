package com.ziyara.backend.domain.enums;

/**
 * Complaint Status Enumeration
 */
public enum ComplaintStatus {
    SUBMITTED("Submitted"),
    ACKNOWLEDGED("Acknowledged"),
    ASSIGNED("Assigned"),
    IN_PROGRESS("In Progress"),
    PENDING_INFO("Pending Info"),
    ESCALATED("Escalated"),
    RESOLVED("Resolved"),
    REJECTED("Rejected"),
    CLOSED("Closed"),
    REOPENED("Reopened");

    private final String displayName;

    ComplaintStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isOpen() {
        return this != CLOSED && this != REJECTED;
    }

    public boolean canBeAssigned() {
        return this == SUBMITTED || this == ACKNOWLEDGED;
    }

    public boolean canBeResolved() {
        return this == IN_PROGRESS || this == ESCALATED;
    }
}
