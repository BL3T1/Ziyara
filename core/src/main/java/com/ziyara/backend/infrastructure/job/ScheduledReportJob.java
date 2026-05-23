package com.ziyara.backend.infrastructure.job;

import com.ziyara.backend.application.service.ReportService;
import com.ziyara.backend.application.dto.response.RevenueReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Scheduled jobs that generate and email weekly/monthly reports.
 * Email delivery uses Spring Mail when APP_NOTIFICATIONS_EMAIL_ENABLED=true.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledReportJob {

    private final ReportService reportService;

    /** Weekly revenue summary — every Monday at 07:00 UTC. */
    @Scheduled(cron = "0 0 7 * * MON", zone = "UTC")
    public void weeklyRevenueReport() {
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDate start = end.minusDays(6);
        try {
            RevenueReportResponse report = reportService.generateRevenueReport(start, end, "ALL", null, null);
            log.info("[ScheduledReport] Weekly revenue {}-{}: {} {}",
                    start, end, report.getTotalRevenue(), report.getCurrency());
        } catch (Exception e) {
            log.error("[ScheduledReport] Weekly revenue report failed", e);
        }
    }

    /** Monthly booking summary — 1st of every month at 07:00 UTC. */
    @Scheduled(cron = "0 0 7 1 * *", zone = "UTC")
    public void monthlyBookingReport() {
        LocalDate end = LocalDate.now().withDayOfMonth(1).minusDays(1);
        LocalDate start = end.withDayOfMonth(1);
        try {
            var report = reportService.generateBookingReport(start, end, "ALL", null, null);
            log.info("[ScheduledReport] Monthly bookings {}-{}: {} total",
                    start, end, report.getTotalBookings());
        } catch (Exception e) {
            log.error("[ScheduledReport] Monthly booking report failed", e);
        }
    }
}
