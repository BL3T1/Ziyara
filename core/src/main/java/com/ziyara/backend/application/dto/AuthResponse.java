package com.ziyara.backend.application.dto;

import com.ziyara.backend.domain.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO: AuthResponse
 * Response body for successful authentication
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication response")
public class AuthResponse {
    
    @Schema(description = "Access token")
    private String accessToken;
    
    @Schema(description = "Refresh token")
    private String refreshToken;
    
    @Schema(description = "Token type", example = "Bearer")
    private String tokenType = "Bearer";
    
    @Schema(description = "Token expiration in seconds")
    private Long expiresIn;
    
    @Schema(description = "User ID")
    private UUID userId;
    
    @Schema(description = "User email")
    private String email;
    
    @Schema(description = "User role")
    private UserRole role;
    
    @Schema(description = "User full name")
    private String fullName;
}
