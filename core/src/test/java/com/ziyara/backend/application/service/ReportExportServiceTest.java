package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.BookingReportResponse;
import com.ziyara.backend.application.dto.response.RevenueReportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportExportServiceTest {

    ReportExportService service;

    @BeforeEach
    void setUp() {
        service = new ReportExportService();
    }

    // ── exportRevenueToCsv ────────────────────────────────────────────────────

    @Test
    void exportRevenueToCsv_containsHeaderAndRows() {
        RevenueReportResponse report = RevenueReportResponse.builder()
                .start(LocalDate.of(2024, 1, 1))
                .end(LocalDate.of(2024, 1, 3))
                .totalRevenue(BigDecimal.valueOf(500))
                .currency("USD")
                .byDay(List.of(
                        new RevenueReportResponse.DayTotal(LocalDate.of(2024, 1, 1), BigDecimal.valueOf(200)),
                        new RevenueReportResponse.DayTotal(LocalDate.of(2024, 1, 2), BigDecimal.valueOf(300))
                ))
                .build();

        String csv = new String(service.exportRevenueToCsv(report), StandardCharsets.UTF_8);

        assertThat(csv).startsWith("Date,Amount (USD)");
        assertThat(csv).contains("2024-01-01,200");
        assertThat(csv).contains("2024-01-02,300");
        assertThat(csv).contains("TOTAL,500");
    }

    @Test
    void exportRevenueToCsv_nullDate_usesEmptyString() {
        RevenueReportResponse report = RevenueReportResponse.builder()
                .totalRevenue(BigDecimal.valueOf(100))
                .currency("EUR")
                .byDay(List.of(new RevenueReportResponse.DayTotal(null, BigDecimal.valueOf(100))))
                .build();

        String csv = new String(service.exportRevenueToCsv(report), StandardCharsets.UTF_8);

        // null date should produce an empty cell, not a NPE
        assertThat(csv).contains(",100");
    }

    @Test
    void exportRevenueToCsv_emptyByDay_onlyHeaderAndTotal() {
        RevenueReportResponse report = RevenueReportResponse.builder()
                .totalRevenue(BigDecimal.ZERO)
                .currency("USD")
                .byDay(List.of())
                .build();

        String csv = new String(service.exportRevenueToCsv(report), StandardCharsets.UTF_8);

        assertThat(csv).startsWith("Date,Amount (USD)");
        assertThat(csv).contains("TOTAL,0");
    }

    // ── exportBookingsToCsv ───────────────────────────────────────────────────

    @Test
    void exportBookingsToCsv_containsHeaderAndRows() {
        BookingReportResponse report = BookingReportResponse.builder()
                .start(LocalDate.of(2024, 2, 1))
                .end(LocalDate.of(2024, 2, 2))
                .totalBookings(7)
                .byDay(List.of(
                        new BookingReportResponse.DayCount(LocalDate.of(2024, 2, 1), 3),
                        new BookingReportResponse.DayCount(LocalDate.of(2024, 2, 2), 4)
                ))
                .build();

        String csv = new String(service.exportBookingsToCsv(report), StandardCharsets.UTF_8);

        assertThat(csv).startsWith("Date,Count");
        assertThat(csv).contains("2024-02-01,3");
        assertThat(csv).contains("2024-02-02,4");
        assertThat(csv).contains("TOTAL,7");
    }

    @Test
    void exportBookingsToCsv_emptyDays_onlyHeaderAndTotal() {
        BookingReportResponse report = BookingReportResponse.builder()
                .totalBookings(0)
                .byDay(List.of())
                .build();

        String csv = new String(service.exportBookingsToCsv(report), StandardCharsets.UTF_8);

        assertThat(csv).startsWith("Date,Count");
        assertThat(csv).contains("TOTAL,0");
    }

    // ── exportRevenueToExcel ──────────────────────────────────────────────────

    @Test
    void exportRevenueToExcel_returnsNonEmptyBytes() throws Exception {
        RevenueReportResponse report = RevenueReportResponse.builder()
                .totalRevenue(BigDecimal.valueOf(1000))
                .currency("USD")
                .byDay(List.of(
                        new RevenueReportResponse.DayTotal(LocalDate.of(2024, 1, 1), BigDecimal.valueOf(1000))
                ))
                .build();

        byte[] bytes = service.exportRevenueToExcel(report);

        assertThat(bytes).isNotEmpty();
        // XLSX files start with PK (zip magic bytes)
        assertThat(bytes[0]).isEqualTo((byte) 0x50); // 'P'
        assertThat(bytes[1]).isEqualTo((byte) 0x4B); // 'K'
    }

    @Test
    void exportBookingsToExcel_returnsNonEmptyBytes() throws Exception {
        BookingReportResponse report = BookingReportResponse.builder()
                .totalBookings(5)
                .byDay(List.of(
                        new BookingReportResponse.DayCount(LocalDate.of(2024, 1, 1), 5)
                ))
                .build();

        byte[] bytes = service.exportBookingsToExcel(report);

        assertThat(bytes).isNotEmpty();
        assertThat(bytes[0]).isEqualTo((byte) 0x50);
        assertThat(bytes[1]).isEqualTo((byte) 0x4B);
    }
}
