package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request for current user to change their own password.
 */
@Data
@Schema(description = "Change password request (current user)")
public class ChangePasswordRequest {

    @NotBlank
    @Schema(description = "Current password", requiredMode = Schema.RequiredMode.REQUIRED)
    private String currentPassword;

    @NotBlank
    @Size(min = 6)
    @Schema(description = "New password", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 6)
    private String newPassword;
}
