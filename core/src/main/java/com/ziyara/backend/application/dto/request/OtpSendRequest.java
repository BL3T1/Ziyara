package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request to send OTP to email or phone.
 */
@Data
@Schema(description = "Send OTP request")
public class OtpSendRequest {

    @NotBlank
    @Schema(description = "Email or phone number", requiredMode = Schema.RequiredMode.REQUIRED)
    private String emailOrPhone;
}
