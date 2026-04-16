package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request to verify OTP.
 */
@Data
@Schema(description = "Verify OTP request")
public class OtpVerifyRequest {

    @NotBlank
    @Schema(description = "Email or phone number", requiredMode = Schema.RequiredMode.REQUIRED)
    private String emailOrPhone;

    @NotBlank
    @Schema(description = "OTP code", requiredMode = Schema.RequiredMode.REQUIRED)
    private String otp;
}
