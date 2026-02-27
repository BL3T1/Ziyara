package com.ziyarah.application.dto;

import com.ziyarah.domain.enums.UserRole;
import com.ziyarah.domain.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO: UserResponse
 * Response body for user data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User response")
public class UserResponse {
    
    @Schema(description = "User ID")
    private UUID id;
    
    @Schema(description = "User email")
    private String email;
    
    @Schema(description = "User phone")
    private String phone;
    
    @Schema(description = "User role")
    private UserRole role;
    
    @Schema(description = "User status")
    private UserStatus status;
    
    @Schema(description = "Email verified flag")
    private Boolean emailVerified;
    
    @Schema(description = "Phone verified flag")
    private Boolean phoneVerified;
    
    @Schema(description = "First name")
    private String firstName;
    
    @Schema(description = "Last name")
    private String lastName;
    
    @Schema(description = "Full name")
    private String fullName;
    
    @Schema(description = "Profile image URL")
    private String profileImageUrl;
    
    @Schema(description = "Preferred currency")
    private String preferredCurrency;
    
    @Schema(description = "Last login time")
    private LocalDateTime lastLoginAt;
    
    @Schema(description = "Account created at")
    private LocalDateTime createdAt;
}
