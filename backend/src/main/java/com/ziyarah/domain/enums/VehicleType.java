package com.ziyarah.domain.enums;

/**
 * Vehicle Type Enumeration
 * Categorizes vehicles for taxi services
 */
public enum VehicleType {
    STANDARD("Standard Sedan"),
    PREMIUM("Premium Sedan"),
    SUV("SUV"),
    VAN("Luxury Van"),
    MINIBUS("Minibus"),
    BUS("Full-size Bus"),
    ELECTRIC("Electric Vehicle");

    private final String displayName;

    VehicleType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
