package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * One row for company dashboard “create user” role picklist: built-in {@link com.ziyara.backend.domain.enums.UserRole}
 * rows and active custom {@code sys_roles} (RBAC).
 */
@Value
@Builder
@Schema(description = "Assignable company staff role option (system enum row or custom RBAC role)")
public class StaffDirectoryRoleOptionResponse {

    @Schema(description = "SYSTEM = enum-backed sys_roles row; CUSTOM = custom RBAC role (system_role = false)")
    String source;

    @Schema(description = "sys_roles.id — send as primaryRbacRoleId on POST /users for either source")
    UUID rbacRoleId;

    @Schema(
            description = "Value stored on sys_users.role and JWT \"role\" claim when this option is chosen. "
                    + "For CUSTOM roles derived from RoleLevel: EXECUTIVE→CEO, MANAGER→GENERAL_MANAGER, EMPLOYEE→SALES_REPRESENTATIVE.")
    String securityUserRole;

    @Schema(description = "UserRole enum name or custom code (display grouping / legacy)")
    String code;

    @Schema(description = "Localized role label from sys_roles")
    String displayName;

    @Schema(description = "Organizational group id from sys_roles.group_id")
    UUID groupId;

    @Schema(description = "Localized group name from sys_groups")
    String groupName;

    @Schema(description = "Group code e.g. G2")
    String groupCode;
}
