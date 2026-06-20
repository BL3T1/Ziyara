package com.ziyara.backend.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO: AuthRequest
 * Request body for authentication
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication request")
public class AuthRequest {
    
    @NotBlank(message = "Identifier is required")
    @Schema(description = "Username (company staff) or email address (provider portal)", example = "john.smith")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    @Schema(description = "User password", example = "Password123!")
    private String password;
    
    @Schema(description = "Remember me flag", example = "false")
    private Boolean rememberMe = false;

    @Schema(description = "TOTP code when MFA is enabled on the account", example = "123456")
    private String mfaCode;
}
