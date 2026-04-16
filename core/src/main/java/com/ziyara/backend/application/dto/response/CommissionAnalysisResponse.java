package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dashboard: aggregate base amount vs commission in date range")
public class CommissionAnalysisResponse {

    @Schema(description = "Start date of range")
    private LocalDate start;

    @Schema(description = "End date of range")
    private LocalDate end;

    @Schema(description = "Total base amount (bookings)")
    private BigDecimal totalBaseAmount;

    @Schema(description = "Total commission amount")
    private BigDecimal totalCommissionAmount;

    @Schema(description = "Currency")
    private String currency;
}
