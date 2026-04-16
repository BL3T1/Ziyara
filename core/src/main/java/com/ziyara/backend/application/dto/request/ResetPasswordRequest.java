package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request for password reset with token (from forgot-password flow).
 */
@Data
@Schema(description = "Reset password with token")
public class ResetPasswordRequest {

    @NotBlank
    @Schema(description = "Reset token sent to user", requiredMode = Schema.RequiredMode.REQUIRED)
    private String token;

    @NotBlank
    @Size(min = 6)
    @Schema(description = "New password", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 6)
    private String newPassword;
}
