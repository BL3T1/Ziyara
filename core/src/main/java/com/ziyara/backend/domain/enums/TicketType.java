package com.ziyara.backend.domain.enums;

/**
 * Enum for internal ticket types
 */
public enum TicketType {
    BUG_REPORT("Bug Report", "Software bugs and errors"),
    FEATURE_REQUEST("Feature Request", "New feature requests"),
    SYSTEM_ISSUE("System Issue", "System-level problems"),
    ACCESS_REQUEST("Access Request", "Access and permission requests"),
    DATA_REQUEST("Data Request", "Data export or modification requests"),
    GENERAL_INQUIRY("General Inquiry", "General questions and inquiries"),
    MAINTENANCE("Maintenance", "Scheduled maintenance tasks"),
    SECURITY_ISSUE("Security Issue", "Security-related issues");

    private final String displayName;
    private final String description;

    TicketType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
