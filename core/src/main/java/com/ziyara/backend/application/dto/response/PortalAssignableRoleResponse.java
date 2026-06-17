package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * One row for provider portal "create staff user" role picklist — custom roles flagged
 * {@code is_provider_role = true} by an admin.
 */
@Value
@Builder
@Schema(description = "Role assignable to provider portal staff")
public class PortalAssignableRoleResponse {

    @Schema(description = "sys_roles.id — send as roleId on POST /portal/staff/users")
    UUID id;

    @Schema(description = "Role code")
    String code;

    @Schema(description = "Localized role name")
    String name;
}
