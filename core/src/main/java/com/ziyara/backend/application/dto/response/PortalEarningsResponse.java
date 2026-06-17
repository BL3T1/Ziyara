package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Provider portal earnings summary — includes profit-share breakdown and per-service detail.
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

    /** Backward-compat alias for grossRevenue. */
    @Schema(description = "Total completed payment amount (= grossRevenue)")
    private BigDecimal totalEarnings;

    @Schema(description = "Currency")
    private String currency;

    // ── Transparency fields ──────────────────────────────────────────────────

    @Schema(description = "Total customer payments collected (gross)")
    private BigDecimal grossRevenue;

    @Schema(description = "Platform commission percentage applied (e.g. 10.0 = 10%)")
    private BigDecimal platformCommissionPct;

    @Schema(description = "Platform fee = grossRevenue × platformCommissionPct / 100")
    private BigDecimal platformFee;

    @Schema(description = "Provider net = grossRevenue − platformFee")
    private BigDecimal providerNet;

    @Schema(description = "Net provider earnings minus pending payout requests — the amount available to request as payout")
    private BigDecimal availableForPayout;

    @Schema(description = "Total booking count across all services")
    private int bookingCount;

    @Schema(description = "Per-service earnings breakdown (up to 20 services)")
    private List<ServiceEarningRow> perServiceBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Earnings row for a single service listing")
    public static class ServiceEarningRow {
        private UUID serviceId;
        private String serviceName;
        private int bookingCount;
        private BigDecimal grossRevenue;
        private BigDecimal platformFee;
        private BigDecimal providerNet;
    }
}
