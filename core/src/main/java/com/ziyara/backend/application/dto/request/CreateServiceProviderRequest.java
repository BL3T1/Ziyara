package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create a partner (service provider). Use managerEmail to link existing PROVIDER_MANAGER, or managerEmail + managerPassword to create a new manager login.")
public class CreateServiceProviderRequest {

    @NotBlank(message = "Name is required")
    @Schema(description = "Provider name")
    private String name;
    
    @Schema(description = "Partner type; must match ServiceType enum: HOTEL, RESORT, RESTAURANT, TAXI, TRIP")
    private String type;
    
    @Schema(description = "Registration or license number")
    private String registrationNumber;
    
    @NotBlank(message = "Phone number is required")
    @Schema(description = "Contact phone")
    private String phone;
    
    @Email(message = "Invalid email format")
    @Schema(description = "Public/contact email for the provider profile")
    private String email;

    @Email(message = "Invalid email format")
    @Schema(description = "Login email for the new provider manager (when creating user; may match contact email)")
    private String managerEmail;

    @Size(min = 6, message = "Manager password must be at least 6 characters")
    @Schema(description = "Initial password for the new provider manager user")
    private String managerPassword;

    @Schema(description = "Phone for the new provider manager user (optional)")
    private String managerPhone;

    @NotBlank(message = "Address is required")
    @Schema(description = "Provider address")
    private String address;
    
    @Schema(description = "Provider description")
    private String description;

    @Schema(description = "Logo image URL (optional)")
    private String logoUrl;

    @Schema(description = "Initial subscription plan: FREE (default) or PRO", allowableValues = {"FREE", "PRO"})
    private String subscriptionPlan;

    @Schema(description = "Staff limit for PRO plan (ignored for FREE, defaults to 10)")
    private Integer staffLimit;

    @NotNull(message = "Global rate is required")
    @DecimalMin(value = "1.0", message = "Global rate must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Global rate must be at most 5.0")
    @Schema(description = "Official classification (e.g. 3.0 = 3-star, 4.5 = 4.5-star). Range 1.0–5.0")
    private BigDecimal globalRate;
}
