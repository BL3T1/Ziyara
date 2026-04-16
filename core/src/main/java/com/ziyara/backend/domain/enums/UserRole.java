package com.ziyara.backend.domain.enums;

import java.util.List;

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

    /**
     * Internal company staff shown in Management → Users (excludes B2C customers and provider/partner logins).
     */
    public boolean isCompanyDirectoryUser() {
        return switch (this) {
            case CUSTOMER, PROVIDER_MANAGER, PROVIDER_FINANCE, PROVIDER_STAFF, TAXI_OPERATOR -> false;
            default -> true;
        };
    }

    /**
     * Roles allowed when HR/Super Admin creates or reassigns a user from the <strong>company</strong> dashboard.
     * Excludes B2C {@link #CUSTOMER}, provider-portal roles, and break-glass {@link #SUPER_ADMIN} (created via ops/seeding only).
     * Provider portal account creation stays on portal endpoints with provider-scoped roles.
     */
    public boolean isCompanyDashboardCreatable() {
        return isCompanyDirectoryUser() && this != SUPER_ADMIN;
    }

    /** DB {@code role} values excluded from paginated company user list. */
    public static List<String> companyDirectoryExcludedRoleNames() {
        return List.of(
                CUSTOMER.name(),
                PROVIDER_MANAGER.name(),
                PROVIDER_FINANCE.name(),
                PROVIDER_STAFF.name(),
                TAXI_OPERATOR.name()
        );
    }

    public boolean isManager() {
        return this == SUPER_ADMIN || this == SALES_MANAGER || this == FINANCE_MANAGER 
            || this == SUPPORT_MANAGER || this == CEO || this == GENERAL_MANAGER 
            || this == HR_MANAGER || this == PROVIDER_MANAGER;
    }

    public boolean canApproveDiscounts() {
        return this == SUPER_ADMIN || this == CEO;
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
