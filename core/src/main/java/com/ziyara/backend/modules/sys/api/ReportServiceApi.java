package com.ziyara.backend.modules.sys.api;

import com.ziyara.backend.application.dto.response.BookingReportResponse;
import com.ziyara.backend.application.dto.response.RevenueReportResponse;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Report module API (part of sys).
 * Consumers (scheduled jobs, export) must depend only on this interface.
 */
public interface ReportServiceApi {

    RevenueReportResponse generateRevenueReport(LocalDate start, LocalDate end,
                                                String scope, UUID providerId, UUID customerId);

    BookingReportResponse generateBookingReport(LocalDate start, LocalDate end,
                                                String scope, UUID providerId, UUID customerId);
}
