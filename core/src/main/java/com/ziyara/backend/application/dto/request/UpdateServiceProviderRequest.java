package com.ziyara.backend.application.dto.request;

import com.ziyara.backend.domain.enums.ProviderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
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
    
    @Schema(description = "Commission rate override (e.g. 10.5 = 10.5%). Null = keep current")
    private BigDecimal commissionRate;
}
