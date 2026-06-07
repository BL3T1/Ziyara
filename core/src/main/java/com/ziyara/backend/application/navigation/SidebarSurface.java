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
    PROVIDER,
    USER;

    public static SidebarSurface fromUserRole(UserRole role) {
        if (role == null) {
            return USER;
        }
        return switch (role) {
            case SUPER_ADMIN -> SUPER_ADMIN;
            case STAFF -> ADMIN;
            case CUSTOMER -> USER;
        };
    }
}
