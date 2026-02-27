package com.ziyarah.domain.enums;

/**
 * User Role Enumeration
 * Defines all possible user roles in the system
 */
public enum UserRole {
    CUSTOMER("Customer"),
    SUPER_ADMIN("Super Admin"),
    SALES_MANAGER("Sales Manager"),
    SALES_REPRESENTATIVE("Sales Representative"),
    FINANCE_MANAGER("Finance Manager"),
    ACCOUNTANT("Accountant"),
    SUPPORT_MANAGER("Support Manager"),
    SUPPORT_AGENT("Support Agent"),
    CEO("CEO"),
    GENERAL_MANAGER("General Manager"),
    HR_MANAGER("HR Manager"),
    PROVIDER_MANAGER("Provider Manager"),
    PROVIDER_FINANCE("Provider Finance"),
    PROVIDER_STAFF("Provider Staff"),
    TAXI_OPERATOR("Taxi Operator");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEmployee() {
        return this != CUSTOMER;
    }

    public boolean isManager() {
        return this == SUPER_ADMIN || this == SALES_MANAGER || this == FINANCE_MANAGER 
            || this == SUPPORT_MANAGER || this == CEO || this == GENERAL_MANAGER 
            || this == HR_MANAGER || this == PROVIDER_MANAGER;
    }

    public boolean canApproveDiscounts() {
        return this == SUPER_ADMIN || this == SALES_MANAGER || this == FINANCE_MANAGER 
            || this == CEO || this == GENERAL_MANAGER;
    }

    public int getMaxDiscountPercentage() {
        return switch (this) {
            case SALES_REPRESENTATIVE -> 20;
            case ACCOUNTANT -> 30;
            case FINANCE_MANAGER -> 50;
            case SUPER_ADMIN, CEO, GENERAL_MANAGER -> 100;
            default -> 0;
        };
    }
}
