package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Report: revenue totals and by day in date range")
public class RevenueReportResponse {

    @Schema(description = "Start date")
    private LocalDate start;

    @Schema(description = "End date")
    private LocalDate end;

    @Schema(description = "Total revenue in range")
    private BigDecimal totalRevenue;

    @Schema(description = "Currency")
    private String currency;

    @Schema(description = "Revenue per day")
    private List<DayTotal> byDay;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DayTotal {
        private LocalDate date;
        private BigDecimal amount;
    }
}
