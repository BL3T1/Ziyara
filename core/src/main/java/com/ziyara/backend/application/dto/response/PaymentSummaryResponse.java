package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

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

    @Schema(description = "Currency code (ISO 4217) — primary currency for scalar totals")
    private String currency;

    @Schema(description = "Collected amounts broken down by currency code")
    private Map<String, BigDecimal> collectedByCurrency;

    @Schema(description = "Pending amounts broken down by currency code")
    private Map<String, BigDecimal> pendingByCurrency;

    @Schema(description = "Refunded amounts broken down by currency code")
    private Map<String, BigDecimal> refundedByCurrency;
}
