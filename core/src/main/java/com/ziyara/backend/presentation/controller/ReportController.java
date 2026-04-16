package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.UserResponse;
import com.ziyara.backend.application.dto.response.BookingReportResponse;
import com.ziyara.backend.application.dto.response.RevenueReportResponse;
import com.ziyara.backend.application.service.ReportService;
import com.ziyara.backend.application.service.SuperAdminRecoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'FINANCE_MANAGER', 'GENERAL_MANAGER', 'CEO')")
@Tag(name = "Reports", description = "Revenue and booking reports")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {
    private final ReportService reportService;
    private final SuperAdminRecoveryService superAdminRecoveryService;

    @GetMapping("/revenue")
    @Operation(
            summary = "Revenue report",
            description = "Totals and by-day revenue in date range, converted to platform default currency when rates exist. "
                    + "scope=ALL (default), PROVIDER (requires providerId), CUSTOMER (requires customerId).")
    public ResponseEntity<ApiResponse<RevenueReportResponse>> generateRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "ALL") String scope,
            @RequestParam(required = false) UUID providerId,
            @RequestParam(required = false) UUID customerId) {
        validateReportScope(scope, providerId, customerId);
        return ResponseEntity.ok(
                ApiResponse.success(reportService.generateRevenueReport(start, end, scope, providerId, customerId)));
    }

    @GetMapping("/bookings")
    @Operation(
            summary = "Booking report",
            description = "Totals and by-day booking counts. scope=ALL, PROVIDER (providerId), CUSTOMER (customerId).")
    public ResponseEntity<ApiResponse<BookingReportResponse>> generateBookingReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "ALL") String scope,
            @RequestParam(required = false) UUID providerId,
            @RequestParam(required = false) UUID customerId) {
        validateReportScope(scope, providerId, customerId);
        return ResponseEntity.ok(
                ApiResponse.success(reportService.generateBookingReport(start, end, scope, providerId, customerId)));
    }

    @GetMapping("/customer-search")
    @Operation(summary = "Search customers for reports", description = "Find CUSTOMER users by email, phone, or name (for scope=CUSTOMER)")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchCustomersForReport(
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.success(superAdminRecoveryService.searchCustomers(q, limit)));
    }

    private static void validateReportScope(String scope, UUID providerId, UUID customerId) {
        String s = scope == null ? "ALL" : scope.trim().toUpperCase(Locale.ROOT);
        if (!List.of("ALL", "PROVIDER", "CUSTOMER").contains(s)) {
            throw new IllegalArgumentException("scope must be ALL, PROVIDER, or CUSTOMER");
        }
        if ("PROVIDER".equals(s) && providerId == null) {
            throw new IllegalArgumentException("providerId is required when scope is PROVIDER");
        }
        if ("CUSTOMER".equals(s) && customerId == null) {
            throw new IllegalArgumentException("customerId is required when scope is CUSTOMER");
        }
    }
}
