package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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
}
