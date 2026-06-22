package com.ziyara.backend.application.query;

import com.ziyara.backend.application.dto.response.BookingReportResponse;
import com.ziyara.backend.application.dto.response.RevenueReportResponse;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * jOOQ query handler for reports: revenue (with conversion to platform default currency) and bookings.
 * Currency settings are passed by the caller (ReportService) to avoid a query ↔ service cycle.
 */
@Component
@RequiredArgsConstructor
public class ReportQueryHandler {

    private static final String PAYMENTS = "pay_payments";
    private static final String BOOKINGS = "bkg_bookings";
    private static final String SERVICES = "hotel_services";

    private final DSLContext dsl;

    public RevenueReportResponse getRevenueReport(
            LocalDate start,
            LocalDate end,
            String scope,
            UUID providerId,
            UUID customerId,
            String targetCurrency,
            BiFunction<BigDecimal, String, BigDecimal> currencyConverter) {
        LocalDateTime from = (start != null ? start : LocalDate.now().minusMonths(1)).atStartOfDay();
        LocalDateTime to = (end != null ? end : LocalDate.now()).atTime(23, 59, 59);

        Field<LocalDateTime> createdAt = DSL.field(DSL.name(PAYMENTS, "created_at"), LocalDateTime.class);
        Field<BigDecimal> amount = DSL.field(DSL.name(PAYMENTS, "amount"), BigDecimal.class);
        Field<String> status = DSL.field(DSL.name(PAYMENTS, "status"), String.class);
        Field<String> payCurrency = DSL.field(DSL.name(PAYMENTS, "currency"), String.class);
        Field<UUID> bookingId = DSL.field(DSL.name(PAYMENTS, "booking_id"), UUID.class);

        Condition cond = createdAt.ge(from).and(createdAt.le(to)).and(status.eq("COMPLETED"));
        cond = cond.and(buildPaymentScopeCondition(bookingId, scope, providerId, customerId));

        var rows = dsl.select(createdAt, amount, payCurrency)
                .from(DSL.table(DSL.name(PAYMENTS)))
                .where(cond)
                .fetch();

        Map<LocalDate, BigDecimal> byDayMap = new TreeMap<>();
        for (Record r : rows) {
            LocalDateTime ts = r.get(createdAt);
            if (ts == null) {
                continue;
            }
            LocalDate day = ts.toLocalDate();
            BigDecimal amt = r.get(amount);
            if (amt == null) {
                amt = BigDecimal.ZERO;
            }
            String fromCur = r.get(payCurrency);
            if (fromCur == null || fromCur.isBlank()) {
                fromCur = "USD";
            }
            fromCur = fromCur.trim().toUpperCase(Locale.ROOT);
            BigDecimal converted = currencyConverter.apply(amt, fromCur);
            byDayMap.merge(day, converted, BigDecimal::add);
        }

        List<RevenueReportResponse.DayTotal> byDay = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> e : byDayMap.entrySet()) {
            byDay.add(RevenueReportResponse.DayTotal.builder()
                    .date(e.getKey())
                    .amount(e.getValue())
                    .build());
        }

        BigDecimal totalRevenue = byDay.stream()
                .map(RevenueReportResponse.DayTotal::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return RevenueReportResponse.builder()
                .start(from.toLocalDate())
                .end(to.toLocalDate())
                .totalRevenue(totalRevenue)
                .currency(targetCurrency)
                .byDay(byDay)
                .build();
    }

    public BookingReportResponse getBookingReport(
            LocalDate start,
            LocalDate end,
            String scope,
            UUID providerId,
            UUID customerId) {
        LocalDateTime from = (start != null ? start : LocalDate.now().minusMonths(1)).atStartOfDay();
        LocalDateTime to = (end != null ? end : LocalDate.now()).atTime(23, 59, 59);

        Field<LocalDateTime> createdAt = DSL.field(DSL.name(BOOKINGS, "created_at"), LocalDateTime.class);
        Field<LocalDate> dayExpr = DSL.field(DSL.sql("({0}::date)", createdAt), LocalDate.class);
        Field<UUID> serviceId = DSL.field(DSL.name(BOOKINGS, "service_id"), UUID.class);
        Field<UUID> custId = DSL.field(DSL.name(BOOKINGS, "customer_id"), UUID.class);

        Condition bookCond = createdAt.ge(from).and(createdAt.le(to));
        bookCond = bookCond.and(buildBookingScopeCondition(serviceId, custId, scope, providerId, customerId));

        var byDayRows = dsl.select(dayExpr.as("day"), DSL.count().as("cnt"))
                .from(DSL.table(DSL.name(BOOKINGS)))
                .where(bookCond)
                .groupBy(dayExpr)
                .orderBy(dayExpr)
                .fetch();

        List<BookingReportResponse.DayCount> byDay = byDayRows.map(r -> BookingReportResponse.DayCount.builder()
                .date(r.get("day", LocalDate.class))
                .count(r.get("cnt", Long.class) != null ? r.get("cnt", Long.class) : 0L)
                .build());

        long totalBookings = byDay.stream().mapToLong(BookingReportResponse.DayCount::getCount).sum();

        return BookingReportResponse.builder()
                .start(from.toLocalDate())
                .end(to.toLocalDate())
                .totalBookings(totalBookings)
                .byDay(byDay)
                .build();
    }

    private Condition buildPaymentScopeCondition(
            Field<UUID> bookingIdField,
            String scope,
            UUID providerId,
            UUID customerId) {
        String s = scope != null ? scope.trim().toUpperCase(Locale.ROOT) : "ALL";
        if ("PROVIDER".equals(s) && providerId != null) {
            Field<UUID> bid = DSL.field(DSL.name(BOOKINGS, "id"), UUID.class);
            Field<UUID> sid = DSL.field(DSL.name(SERVICES, "id"), UUID.class);
            Field<UUID> pid = DSL.field(DSL.name(SERVICES, "provider_id"), UUID.class);
            Field<UUID> bsvc = DSL.field(DSL.name(BOOKINGS, "service_id"), UUID.class);
            var sub = DSL.select(bid)
                    .from(DSL.table(DSL.name(BOOKINGS)))
                    .join(DSL.table(DSL.name(SERVICES))).on(bsvc.eq(sid))
                    .where(pid.eq(providerId));
            return bookingIdField.in(sub);
        }
        if ("CUSTOMER".equals(s) && customerId != null) {
            Field<UUID> bid = DSL.field(DSL.name(BOOKINGS, "id"), UUID.class);
            Field<UUID> cid = DSL.field(DSL.name(BOOKINGS, "customer_id"), UUID.class);
            var sub = DSL.select(bid).from(DSL.table(DSL.name(BOOKINGS))).where(cid.eq(customerId));
            return bookingIdField.in(sub);
        }
        return DSL.noCondition();
    }

    private Condition buildBookingScopeCondition(
            Field<UUID> serviceIdField,
            Field<UUID> customerIdField,
            String scope,
            UUID providerId,
            UUID customerId) {
        String s = scope != null ? scope.trim().toUpperCase(Locale.ROOT) : "ALL";
        if ("PROVIDER".equals(s) && providerId != null) {
            Field<UUID> sid = DSL.field(DSL.name(SERVICES, "id"), UUID.class);
            Field<UUID> pid = DSL.field(DSL.name(SERVICES, "provider_id"), UUID.class);
            var sub = DSL.select(sid).from(DSL.table(DSL.name(SERVICES))).where(pid.eq(providerId));
            return serviceIdField.in(sub);
        }
        if ("CUSTOMER".equals(s) && customerId != null) {
            return customerIdField.eq(customerId);
        }
        return DSL.noCondition();
    }
}
