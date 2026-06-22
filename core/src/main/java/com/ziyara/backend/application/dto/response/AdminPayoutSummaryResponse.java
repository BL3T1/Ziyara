package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Summary metrics for the Finance > Payouts page")
public class AdminPayoutSummaryResponse {

    @Schema(description = "Total net payable across all PENDING rows")
    private BigDecimal totalPayable;

    @Schema(description = "Number of PENDING rows")
    private long pendingCount;

    @Schema(description = "Number of PROCESSING rows")
    private long processingCount;

    @Schema(description = "Total amount COMPLETED in the selected period")
    private BigDecimal totalCompletedInPeriod;

    @Schema(description = "Number of FAILED + ON_HOLD rows")
    private long failedOnHoldCount;

    @Schema(description = "Currency for monetary values (always USD)")
    private String currency;
}
