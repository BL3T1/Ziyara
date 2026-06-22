package com.ziyara.backend.domain.enums;

/**
 * Coarse user tier stored in sys_users.role.
 *
 * SUPER_ADMIN – hardcoded; gets all permissions from the SUPER_ADMIN sys_role.
 * CUSTOMER    – B2C mobile/web user; no company dashboard access.
 * STAFF       – every company employee and portal partner; permissions come
 *               exclusively from sys_user_roles → sys_role_permissions.
 */
public enum UserRole {
    SUPER_ADMIN,
    CUSTOMER,
    STAFF;

    public boolean isStaff() {
        return this == STAFF;
    }

    public boolean isSuperAdmin() {
        return this == SUPER_ADMIN;
    }

    public boolean isCustomer() {
        return this == CUSTOMER;
    }
}
