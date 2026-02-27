package com.ziyarah.domain.enums;

/**
 * Taxi Status Enumeration
 * Specific states for taxi bookings
 */
public enum TaxiStatus {
    SEARCHING("Searching for Driver"),
    ASSIGNED("Driver Assigned"),
    EN_ROUTE_TO_PICKUP("En Route to Pickup"),
    ARRIVED_AT_PICKUP("Arrived at Pickup"),
    IN_PROGRESS("Trip in Progress"),
    ARRIVED_AT_DESTINATION("Arrived at Destination"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    NO_DRIVER_FOUND("No Driver Found");

    private final String displayName;

    TaxiStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
