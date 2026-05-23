package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Provider payout request acknowledgement")
public class PayoutRequestResponse {

    @Schema(description = "Payout request ID")
    private UUID id;

    @Schema(description = "Requested amount")
    private BigDecimal amount;

    @Schema(description = "Currency code")
    private String currency;

    @Schema(description = "Status: PENDING | PROCESSING | COMPLETED | REJECTED")
    private String status;

    @Schema(description = "Timestamp the request was submitted")
    private Instant requestedAt;
}
