package com.ziyara.backend.application.dto.request;

import com.ziyara.backend.domain.enums.ProviderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update provider details")
public class UpdateServiceProviderRequest {
    
    @Schema(description = "Updated name")
    private String name;
    
    @Schema(description = "Updated phone")
    private String phone;
    
    @Schema(description = "Updated email")
    private String email;
    
    @Schema(description = "Updated address")
    private String address;
    
    @Schema(description = "Updated description")
    private String description;
    
    @Schema(description = "Updated status")
    private ProviderStatus status;
    
    @Schema(description = "Updated logo URL")
    private String logoUrl;
    
    @Schema(description = "Verification status")
    private Boolean verified;
    
    @Schema(description = "Profit margin override (e.g. 10.5 = 10.5%). Null = keep current")
    private BigDecimal profitMargin;

    @Schema(description = "Official classification (e.g. 3.0 = 3-star). Range 1.0–5.0. Null = keep current")
    private BigDecimal globalRate;

    @Future(message = "Expiry date must be in the future")
    @Schema(description = "Updated account expiry date (YYYY-MM-DD). Must be a future date. Null = keep current.")
    private LocalDate expiryDate;
}
