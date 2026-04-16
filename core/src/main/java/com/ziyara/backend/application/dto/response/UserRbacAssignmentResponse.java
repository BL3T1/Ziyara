package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@Schema(description = "Current sys_user_roles assignment for a user, if any")
public class UserRbacAssignmentResponse {
    @Schema(description = "Null when no assignment")
    private UUID roleId;
    private String roleCode;
    private String roleName;
}
