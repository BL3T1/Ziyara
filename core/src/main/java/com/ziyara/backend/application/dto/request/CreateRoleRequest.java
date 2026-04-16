package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "Create custom role (Super Admin only)")
public class CreateRoleRequest {
    @NotBlank
    @Size(max = 50)
    @Schema(description = "Role name", example = "Custom Sales Lead", required = true)
    private String name;

    @Size(max = 500)
    @Schema(description = "Role description")
    private String description;

    @Schema(description = "Permission IDs from catalogue (locked permissions are rejected)")
    private List<UUID> permissionIds;

    @Schema(description = "Optional group ID")
    private UUID groupId;

    @Size(max = 100)
    @Schema(description = "Optional group name to create and bind this role to (used when groupId is omitted)")
    private String createGroupName;
}
