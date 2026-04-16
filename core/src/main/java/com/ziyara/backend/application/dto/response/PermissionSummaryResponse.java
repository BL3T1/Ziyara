package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Permission summary (resource:action)")
public class PermissionSummaryResponse {
    @Schema(description = "Permission ID") private UUID id;
    @Schema(description = "Code (e.g. provider:approve)") private String code;
    @Schema(description = "Display name") private String name;
    @Schema(description = "Resource") private String resource;
    @Schema(description = "Action") private String action;
    @Schema(description = "Cannot be assigned to custom roles") private boolean locked;
}
