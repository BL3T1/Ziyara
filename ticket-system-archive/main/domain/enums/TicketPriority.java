package com.ziyara.backend.domain.enums;

/**
 * Enum for internal ticket priority levels
 */
public enum TicketPriority {
    LOW(1, "Low", "Can be addressed in regular workflow"),
    MEDIUM(2, "Medium", "Should be addressed within standard timeframe"),
    HIGH(3, "High", "Requires prompt attention"),
    CRITICAL(4, "Critical", "Blocking important functionality"),
    URGENT(5, "Urgent", "System down or security issue - immediate action required");

    private final int level;
    private final String displayName;
    private final String description;

    TicketPriority(int level, String displayName, String description) {
        this.level = level;
        this.displayName = displayName;
        this.description = description;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isHigherThan(TicketPriority other) {
        return this.level > other.level;
    }

    public boolean requiresImmediateAttention() {
        return this.level >= CRITICAL.level;
    }
}
