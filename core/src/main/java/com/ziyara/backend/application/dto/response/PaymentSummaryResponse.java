package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@Schema(description = "Platform-wide payment aggregate totals")
public class PaymentSummaryResponse {

    @Schema(description = "Sum of all COMPLETED payments")
    private BigDecimal totalCollected;

    @Schema(description = "Sum of all PENDING payments")
    private BigDecimal totalPending;

    @Schema(description = "Sum of all REFUNDED payments")
    private BigDecimal totalRefunded;

    @Schema(description = "Currency code (ISO 4217)")
    private String currency;
}
