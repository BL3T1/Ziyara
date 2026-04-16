package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request for admin-initiated password reset for a user.
 */
@Data
@Schema(description = "Reset password request (admin)")
public class ResetPasswordAdminRequest {

    @NotBlank
    @Size(min = 6)
    @Schema(description = "New password", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 6)
    private String newPassword;
}
