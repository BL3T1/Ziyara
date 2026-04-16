package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request for forgot-password (send reset link/OTP).
 */
@Data
@Schema(description = "Forgot password request")
public class ForgotPasswordRequest {

    @NotBlank
    @Email
    @Schema(description = "User email", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;
}
