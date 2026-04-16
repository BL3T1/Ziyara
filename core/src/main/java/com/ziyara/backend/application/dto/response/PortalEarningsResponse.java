package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Portal earnings summary for the current provider (BACKEND_CRUD_REPORT §4).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Provider portal earnings summary")
public class PortalEarningsResponse {

    @Schema(description = "Period start (optional filter)")
    private LocalDate start;

    @Schema(description = "Period end (optional filter)")
    private LocalDate end;

    @Schema(description = "Total completed payment amount for provider's bookings")
    private BigDecimal totalEarnings;

    @Schema(description = "Currency")
    private String currency;
}
