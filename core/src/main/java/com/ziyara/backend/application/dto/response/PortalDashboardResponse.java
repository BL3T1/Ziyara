package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Portal dashboard KPIs for the current provider (BACKEND_CRUD_REPORT §4).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Provider portal dashboard KPIs")
public class PortalDashboardResponse {

    @Schema(description = "Number of services (listings)")
    private long serviceCount;

    @Schema(description = "Total bookings for provider's services")
    private long totalBookings;

    @Schema(description = "Active/confirmed bookings count")
    private long activeBookings;

    @Schema(description = "Total revenue (completed payments) for provider")
    private BigDecimal totalRevenue;

    @Schema(description = "Currency for revenue")
    private String revenueCurrency;

    @Schema(description = "Earnings per ISO week for the last 8 weeks (oldest first)")
    private List<WeeklyRevenueItem> weeklyRevenue;

    @Schema(description = "Bookings created in the last 30 days")
    private long bookingsLast30Days;

    @Schema(description = "Bookings created 30–60 days ago (for trend comparison)")
    private long bookingsPrev30Days;

    @Schema(description = "Revenue from completed payments in the last 30 days")
    private BigDecimal revenueLast30Days;

    @Schema(description = "Revenue from completed payments 30–60 days ago")
    private BigDecimal revenuePrev30Days;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyRevenueItem {
        @Schema(description = "ISO week start date (Monday), e.g. 2025-05-12")
        private String week;
        @Schema(description = "Total completed payment amount for that week")
        private BigDecimal amount;
    }
}
