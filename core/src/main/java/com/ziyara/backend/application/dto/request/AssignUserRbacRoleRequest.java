package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Assign at most one RBAC role for custom sidebar; omit or null roleId to clear")
public class AssignUserRbacRoleRequest {
    @Schema(description = "sys_roles.id (typically custom role). Null clears assignment.")
    private UUID roleId;
}
