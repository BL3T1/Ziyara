package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Dashboard KPI metrics (Command Center â€“ DASHBOARD_DESIGN_REPORT).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dashboard KPI metrics")
public class DashboardKpiResponse {

    @Schema(description = "Total revenue (completed payments) in period or all-time")
    private BigDecimal totalRevenue;

    @Schema(description = "Currency for revenue")
    private String revenueCurrency;

    @Schema(description = "Count of active/confirmed bookings")
    private long activeBookings;

    @Schema(description = "Total booking count")
    private long totalBookings;

    @Schema(description = "Total service providers")
    private long totalProviders;

    @Schema(description = "Pending/open complaints count")
    private long pendingComplaints;

    @Schema(description = "Open internal tickets count")
    private long openTickets;
}
