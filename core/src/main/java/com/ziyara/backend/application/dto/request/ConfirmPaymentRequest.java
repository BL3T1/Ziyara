package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to confirm payment after 3DS or gateway callback (Phase 2).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Confirm payment after 3DS or gateway callback")
public class ConfirmPaymentRequest {

    @Schema(description = "Transaction reference from gateway", required = true)
    private String reference;

    @Schema(description = "Gateway name (e.g. STRIPE, STUB)", required = true)
    private String gateway;

    @Schema(description = "External gateway reference (for idempotency)")
    private String gatewayReference;

    @Schema(description = "3DS status: AUTHENTICATED, NOT_REQUIRED, FAILED")
    private String threeDsStatus;
}
