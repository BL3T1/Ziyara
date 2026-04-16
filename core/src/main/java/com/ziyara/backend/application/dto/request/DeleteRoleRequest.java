package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.UUID;

@Data
@Schema(description = "Delete custom role; if users are assigned, targetRoleId is required for reassignment")
public class DeleteRoleRequest {
    @Schema(description = "Target role ID to reassign all users to (required when userCount > 0)")
    private UUID targetRoleId;
}
