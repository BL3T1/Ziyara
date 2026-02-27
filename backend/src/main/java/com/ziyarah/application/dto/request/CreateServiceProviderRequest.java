package com.ziyarah.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a service provider")
public class CreateServiceProviderRequest {
    
    @NotNull(message = "User ID is required")
    @Schema(description = "Associated user ID")
    private UUID userId;
    
    @NotBlank(message = "Name is required")
    @Schema(description = "Provider name")
    private String name;
    
    @Schema(description = "Provider type (e.g. HOTEL, TAXI, TOUR)")
    private String type;
    
    @Schema(description = "Registration or license number")
    private String registrationNumber;
    
    @NotBlank(message = "Phone number is required")
    @Schema(description = "Contact phone")
    private String phone;
    
    @Email(message = "Invalid email format")
    @Schema(description = "Contact email")
    private String email;
    
    @Schema(description = "Provider address")
    private String address;
    
    @Schema(description = "Provider description")
    private String description;
}
