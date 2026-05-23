package com.ziyara.backend.application.dto.response;

import com.ziyara.backend.domain.enums.ProviderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Service provider details")
public class ServiceProviderResponse {
    
    @Schema(description = "Provider ID")
    private UUID id;
    
    @Schema(description = "User ID")
    private UUID userId;
    
    @Schema(description = "Provider name")
    private String name;
    
    @Schema(description = "Provider type")
    private String type;
    
    @Schema(description = "Registration number")
    private String registrationNumber;
    
    @Schema(description = "Contact phone")
    private String phone;
    
    @Schema(description = "Contact email")
    private String email;
    
    @Schema(description = "Address")
    private String address;

    @Schema(description = "Public description")
    private String description;

    @Schema(description = "Logo image URL")
    private String logoUrl;
    
    @Schema(description = "Rating 0.00–5.00 (NUMERIC precision)")
    private BigDecimal rating;
    
    @Schema(description = "Review count")
    private Integer reviewCount;
    
    @Schema(description = "Provider status")
    private ProviderStatus status;
    
    @Schema(description = "Verification status")
    private Boolean verified;
    
    @Schema(description = "Commission rate override (e.g. 10 = 10%). Null = platform default 10%")
    private BigDecimal commissionRate;
    
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "User who approved activation")
    private UUID approvedBy;

    @Schema(description = "When the provider was approved")
    private LocalDateTime approvedAt;
}
