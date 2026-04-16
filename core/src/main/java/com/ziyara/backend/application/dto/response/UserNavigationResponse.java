package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Effective company dashboard sidebar item IDs for the current user")
public class UserNavigationResponse {
    @Schema(description = "Ordered sidebar item ids (matches front sidebar.ts)")
    private List<String> visibleItemIds;

    @Schema(description = "rbac_role | default_user_role", example = "default_user_role")
    private String source;

    @Schema(description = "Assigned RBAC role id when source is rbac_role")
    private UUID rbacRoleId;

    @Schema(description = "RBAC role code when source is rbac_role")
    private String rbacRoleCode;

    @Schema(description = "sys_users.role enum name when source is default_user_role")
    private String userRole;
}
