package com.ziyara.backend.domain.enums;

/**
 * Service Type Enumeration
 * Defines all types of bookable services
 */
public enum ServiceType {
    HOTEL("Hotel"),
    RESORT("Resort"),
    RESTAURANT("Restaurant"),
    TAXI("Taxi"),
    TRIP("Trip");

    private final String displayName;

    ServiceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean requiresCheckOut() {
        return this == HOTEL || this == RESORT;
    }

    public boolean supportsTaxiAddOn() {
        return this == HOTEL || this == RESORT || this == TRIP;
    }
}
