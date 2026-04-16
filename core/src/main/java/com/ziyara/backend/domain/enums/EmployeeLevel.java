package com.ziyara.backend.domain.enums;

/**
 * Employee Level Enumeration
 * Defines hierarchy and permissions levels
 */
public enum EmployeeLevel {
    JUNIOR("Junior"),
    MID_LEVEL("Mid-Level"),
    SENIOR("Senior"),
    LEAD("Lead"),
    MANAGER("Manager"),
    DIRECTOR("Director"),
    ADMINISTRATOR("Administrator"),
    SUPER_ADMIN("Super Admin");

    private final String displayName;

    EmployeeLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
