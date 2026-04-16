package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "Replace all permissions for a role (custom roles: unlocked IDs only; system roles: any ID including locked)")
public class UpdateRolePermissionsRequest {
    @Schema(description = "Permission IDs to assign; unknown IDs rejected; locked IDs rejected for custom roles only")
    private List<UUID> permissionIds;
}
