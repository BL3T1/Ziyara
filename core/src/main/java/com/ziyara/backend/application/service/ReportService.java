package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.BookingReportResponse;
import com.ziyara.backend.application.dto.response.RevenueReportResponse;
import com.ziyara.backend.modules.sys.api.ReportServiceApi;
import com.ziyara.backend.application.query.ReportQueryHandler;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Report generation: revenue and bookings reports via jOOQ.
 */
@Service
@RequiredArgsConstructor
public class ReportService implements ReportServiceApi {

    private final ReportQueryHandler reportQueryHandler;
    private final DSLContext dsl;
    private final SystemSettingsService systemSettingsService;
    private final CurrencyService currencyService;

    public RevenueReportResponse generateRevenueReport(
            LocalDate start,
            LocalDate end,
            String scope,
            UUID providerId,
            UUID customerId) {
        String targetCurrency = systemSettingsService.getSettings().getDefaultCurrency();
        return reportQueryHandler.getRevenueReport(
                start, end, scope, providerId, customerId,
                targetCurrency,
                (amt, fromCur) -> currencyService.convertOrKeep(amt, fromCur, targetCurrency));
    }

    public BookingReportResponse generateBookingReport(
            LocalDate start,
            LocalDate end,
            String scope,
            UUID providerId,
            UUID customerId) {
        return reportQueryHandler.getBookingReport(start, end, scope, providerId, customerId);
    }

    public Map<String, Object> getAnalytics(LocalDate start, LocalDate end) {
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.atTime(23, 59, 59);

        // Total completed vs pending payments (paid vs unpaid ratio)
        var paymentStatusCounts = dsl
                .select(DSL.field(DSL.name("pay_payments", "status")), DSL.count())
                .from(DSL.table(DSL.name("pay_payments")))
                .where(DSL.field(DSL.name("pay_payments", "created_at")).ge(from)
                        .and(DSL.field(DSL.name("pay_payments", "created_at")).le(to)))
                .groupBy(DSL.field(DSL.name("pay_payments", "status")))
                .fetchMap(r -> r.get(0, String.class), r -> r.get(1, Long.class));

        // Top 5 providers by booking count (via bkg_bookings → hotel_services join)
        var topProviders = dsl
                .select(
                        DSL.field(DSL.name("hotel_services", "provider_id")),
                        DSL.count().as("booking_count")
                )
                .from(DSL.table(DSL.name("bkg_bookings")))
                .join(DSL.table(DSL.name("hotel_services")))
                .on(DSL.field(DSL.name("bkg_bookings", "service_id"))
                        .eq(DSL.field(DSL.name("hotel_services", "id"))))
                .where(DSL.field(DSL.name("bkg_bookings", "created_at")).ge(from)
                        .and(DSL.field(DSL.name("bkg_bookings", "created_at")).le(to)))
                .groupBy(DSL.field(DSL.name("hotel_services", "provider_id")))
                .orderBy(DSL.field("booking_count").desc())
                .limit(5)
                .fetchMaps();

        // Total revenue sum
        BigDecimal totalRevenue = dsl
                .select(DSL.coalesce(DSL.sum(DSL.field(DSL.name("pay_payments", "amount"), BigDecimal.class)), BigDecimal.ZERO))
                .from(DSL.table(DSL.name("pay_payments")))
                .where(DSL.field(DSL.name("pay_payments", "status")).eq("COMPLETED")
                        .and(DSL.field(DSL.name("pay_payments", "created_at")).ge(from))
                        .and(DSL.field(DSL.name("pay_payments", "created_at")).le(to)))
                .fetchOne(0, BigDecimal.class);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("start", start.toString());
        result.put("end", end.toString());
        result.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        result.put("paymentStatusBreakdown", paymentStatusCounts);
        result.put("topProvidersByBookings", topProviders);
        return result;
    }
}
