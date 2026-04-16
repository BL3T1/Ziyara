package com.ziyara.backend.application.dto.request;

import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.Data;

/**
 * Request DTO for updating user profile (no password update here).
 */
@Data
@Schema(description = "Update user request")
public class UpdateUserRequest {

    @Email
    @Schema(description = "User email")
    private String email;

    @Schema(description = "Phone number")
    private String phone;

    @Schema(description = "User status")
    private UserStatus status;

    /**
     * Company staff role (JWT / {@code sys_users.role}). Super Admin and HR only via {@code PUT /users/{id}};
     * ignored on {@code PUT /users/me}.
     */
    @Schema(description = "Dashboard user role; optional; re-syncs primary RBAC row to canonical sys_roles for this code")
    private UserRole role;
}
