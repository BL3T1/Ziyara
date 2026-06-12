package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.response.ActivityFeedItemResponse;
import com.ziyara.backend.application.dto.response.CommissionAnalysisResponse;
import com.ziyara.backend.application.dto.response.DashboardBootstrapResponse;
import com.ziyara.backend.application.dto.response.DashboardKpiResponse;
import com.ziyara.backend.application.dto.response.DashboardLiveResponse;
import com.ziyara.backend.application.dto.response.PayoutSummaryResponse;
import com.ziyara.backend.application.dto.response.ServiceHealthResponse;
import com.ziyara.backend.application.query.DashboardQueryHandler;
import com.ziyara.backend.application.service.DashboardBootstrapService;
import com.ziyara.backend.application.service.DashboardService;
import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Dashboard KPIs and activity feed (DASHBOARD_DESIGN_REPORT).
 * Serves Company Dashboard (internal staff, org groups Z1-Z6) with revenue, bookings, providers, complaints, activity.
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@PreAuthorize(COMPANY_STAFF)
@Tag(name = "Dashboard", description = "Dashboard KPIs and activity feed")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardQueryHandler dashboardQueryHandler;
    private final DashboardBootstrapService dashboardBootstrapService;

    @GetMapping("/revenue")
    @Operation(summary = "Revenue stats", description = "Returns dashboard KPIs including revenue (REQUIREMENTS: GET /api/v1/dashboard/revenue)")
    public ResponseEntity<ApiResponse<DashboardKpiResponse>> getRevenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getKpis(start, end)));
    }

    @GetMapping("/bookings")
    @Operation(summary = "Booking stats", description = "Returns dashboard KPIs including booking counts")
    public ResponseEntity<ApiResponse<DashboardKpiResponse>> getBookings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getKpis(start, end)));
    }

    @GetMapping("/customers")
    @Operation(summary = "Customer stats", description = "Returns dashboard KPIs (same as revenue for now)")
    public ResponseEntity<ApiResponse<DashboardKpiResponse>> getCustomers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getKpis(start, end)));
    }

    @GetMapping("/providers")
    @Operation(summary = "Provider stats", description = "Returns dashboard KPIs including provider count")
    public ResponseEntity<ApiResponse<DashboardKpiResponse>> getProviders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getKpis(start, end)));
    }

    @GetMapping("/kpis")
    @Operation(summary = "All KPIs", description = "Returns revenue, bookings, providers, pending complaints, open tickets")
    public ResponseEntity<ApiResponse<DashboardKpiResponse>> getKpis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getKpis(start, end)));
    }

    @GetMapping("/bootstrap")
    @Operation(summary = "Dashboard bootstrap", description = "Returns KPIs, activity, service health, commission, and payouts in one response (parallel assembly on server)")
    public CompletableFuture<ResponseEntity<ApiResponse<DashboardBootstrapResponse>>> getBootstrap(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "15") int activityLimit) {
        return dashboardBootstrapService.loadAsync(start, end, activityLimit)
                .thenApply(result -> ResponseEntity.ok(ApiResponse.success(result)));
    }

    @GetMapping("/live")
    @Operation(summary = "Dashboard live refresh", description = "KPIs, activity, and service health only (for polling without recomputing commission/payouts)")
    public CompletableFuture<ResponseEntity<ApiResponse<DashboardLiveResponse>>> getLive(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "15") int activityLimit) {
        return dashboardBootstrapService.loadLiveAsync(start, end, activityLimit)
                .thenApply(result -> ResponseEntity.ok(ApiResponse.success(result)));
    }

    @GetMapping("/activity")
    @Operation(summary = "Activity feed", description = "Live activity stream for dashboard")
    public ResponseEntity<ApiResponse<List<ActivityFeedItemResponse>>> getActivity(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getActivityFeed(limit)));
    }

    @GetMapping("/service-health")
    @Operation(summary = "Service health", description = "Counts per vertical and active bookings per type")
    public ResponseEntity<ApiResponse<ServiceHealthResponse>> getServiceHealth() {
        try {
            return ResponseEntity.ok(ApiResponse.success(dashboardQueryHandler.getServiceHealth()));
        } catch (Throwable e) {
            return ResponseEntity.ok(ApiResponse.success(ServiceHealthResponse.builder()
                    .serviceCountByType(java.util.Map.of())
                    .activeBookingCountByType(java.util.Map.of())
                    .build()));
        }
    }

    @GetMapping("/commission-analysis")
    @Operation(summary = "Commission analysis", description = "Aggregate base vs commission in date range")
    public ResponseEntity<ApiResponse<CommissionAnalysisResponse>> getCommissionAnalysis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        try {
            return ResponseEntity.ok(ApiResponse.success(dashboardQueryHandler.getCommissionAnalysis(start, end)));
        } catch (Throwable e) {
            LocalDate s = start != null ? start : java.time.LocalDate.now().minusMonths(1);
            LocalDate endDate = end != null ? end : java.time.LocalDate.now();
            return ResponseEntity.ok(ApiResponse.success(CommissionAnalysisResponse.builder()
                    .start(s)
                    .end(endDate)
                    .totalBaseAmount(java.math.BigDecimal.ZERO)
                    .totalCommissionAmount(java.math.BigDecimal.ZERO)
                    .currency("USD")
                    .build()));
        }
    }

    @GetMapping("/payouts")
    @Operation(summary = "Provider payouts", description = "Provider payouts in period")
    public ResponseEntity<ApiResponse<PayoutSummaryResponse>> getPayouts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        try {
            return ResponseEntity.ok(ApiResponse.success(dashboardQueryHandler.getPayouts(start, end)));
        } catch (Throwable e) {
            LocalDate s = start != null ? start : java.time.LocalDate.now().minusMonths(1);
            LocalDate endDate = end != null ? end : java.time.LocalDate.now();
            return ResponseEntity.ok(ApiResponse.success(PayoutSummaryResponse.builder()
                    .start(s)
                    .end(endDate)
                    .payouts(java.util.List.of())
                    .build()));
        }
    }
}
