package com.ziyara.backend.application.navigation;

import com.ziyara.backend.domain.enums.UserRole;

/**
 * Mirrors {@code front/my-app/src/types/auth.ts} {@code Role} buckets used for default sidebar layout.
 */
public enum SidebarSurface {
    SUPER_ADMIN,
    ADMIN,
    FINANCE,
    SUPPORT,
    EXECUTIVE,
    HR,
    PROVIDER,
    USER;

    public static SidebarSurface fromUserRole(UserRole role) {
        if (role == null) {
            return USER;
        }
        return switch (role) {
            case SUPER_ADMIN -> SUPER_ADMIN;
            case HR_MANAGER -> HR;
            case CEO -> EXECUTIVE;
            case GENERAL_MANAGER -> ADMIN;
            case SALES_MANAGER, SALES_REPRESENTATIVE -> ADMIN;
            case FINANCE_MANAGER, ACCOUNTANT -> FINANCE;
            case SUPPORT_MANAGER, SUPPORT_AGENT -> SUPPORT;
            case PROVIDER_MANAGER, PROVIDER_FINANCE, PROVIDER_STAFF, TAXI_OPERATOR -> PROVIDER;
            case CUSTOMER -> USER;
        };
    }
}
