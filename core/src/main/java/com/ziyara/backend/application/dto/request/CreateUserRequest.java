package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Create user request")
public class CreateUserRequest {

    @NotBlank
    @Email
    @Schema(description = "User email", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "Login username for company staff (unique, required for company dashboard accounts)")
    private String username;

    @Schema(description = "First name")
    private String firstName;

    @Schema(description = "Last name")
    private String lastName;

    @NotBlank
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    @Schema(description = "Password", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 6)
    private String password;

    @Schema(description = "Phone number")
    private String phone;

    @Schema(description = "Primary sys_roles row (from staff-role-options.rbacRoleId)", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID primaryRbacRoleId;

    @Schema(description = "Initial status; default ACTIVE when omitted (staff can sign in immediately)")
    private String status; // optional: ACTIVE, PENDING_VERIFICATION
}
