package com.ziyara.backend.application.dto.request;

import com.ziyara.backend.domain.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/**
 * Request DTO for creating a user (admin/HR).
 * Provide <strong>exactly one</strong> of {@code role} (enum) or {@code primaryRbacRoleId} ({@code sys_roles.id} from
 * GET /users/staff-role-options {@code rbacRoleId}).
 */
@Data
@Schema(description = "Create user request")
public class CreateUserRequest {

    @NotBlank
    @Email
    @Schema(description = "User email", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    @Schema(description = "Password", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 6)
    private String password;

    @Schema(description = "Phone number")
    private String phone;

    @Schema(
            description = "Internal company UserRole (legacy). Omit when primaryRbacRoleId is set.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private UserRole role;

    @Schema(
            description = "Primary sys_roles row (from staff-role-options.rbacRoleId). Omit when role is set.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private UUID primaryRbacRoleId;

    @Schema(description = "Initial status; default ACTIVE when omitted (staff can sign in immediately)")
    private String status; // optional: ACTIVE, PENDING_VERIFICATION
}
