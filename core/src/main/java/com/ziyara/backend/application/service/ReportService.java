package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.BookingReportResponse;
import com.ziyara.backend.application.dto.response.RevenueReportResponse;
import com.ziyara.backend.application.query.ReportQueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Report generation: revenue and bookings reports via jOOQ.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportQueryHandler reportQueryHandler;

    public RevenueReportResponse generateRevenueReport(
            LocalDate start,
            LocalDate end,
            String scope,
            UUID providerId,
            UUID customerId) {
        return reportQueryHandler.getRevenueReport(start, end, scope, providerId, customerId);
    }

    public BookingReportResponse generateBookingReport(
            LocalDate start,
            LocalDate end,
            String scope,
            UUID providerId,
            UUID customerId) {
        return reportQueryHandler.getBookingReport(start, end, scope, providerId, customerId);
    }
}
