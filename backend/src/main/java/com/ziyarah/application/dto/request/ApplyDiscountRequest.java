package com.ziyarah.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to apply a discount code")
public class ApplyDiscountRequest {
    
    @NotBlank(message = "Discount code is required")
    @Schema(description = "The code to be applied")
    private String code;
}
