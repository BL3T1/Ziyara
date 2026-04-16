package com.ziyara.backend.domain.enums;

/**
 * Role hierarchy level (matches DB employee_level_enum used by roles table).
 */
public enum RoleLevel {
    SUPER_ADMIN,
    MANAGER,
    EMPLOYEE,
    EXECUTIVE
}
