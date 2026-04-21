package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

/**
 * Request to set provider-specific commission rate (DYNAMIC_COMMISSION_REPORT).
 * Super Admin and General Manager can set overrides; changes are audited.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Provider commission rate override (e.g. 10 = 10%)")
public class UpdateProviderCommissionRequest {

    @Schema(description = "Commission percentage (0-100). Null or omit to reset to default 10%")
    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal commissionRate;
}
