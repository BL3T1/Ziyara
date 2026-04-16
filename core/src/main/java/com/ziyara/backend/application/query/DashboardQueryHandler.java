package com.ziyara.backend.application.query;

import com.ziyara.backend.application.dto.response.CommissionAnalysisResponse;
import com.ziyara.backend.application.dto.response.PayoutSummaryResponse;
import com.ziyara.backend.application.dto.response.ServiceHealthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * jOOQ query handler for dashboard extensions: service-health, commission-analysis, payouts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DashboardQueryHandler {

    private static final String SERVICES = "hotel_services";
    private static final String BOOKINGS = "bkg_bookings";
    private static final String SERVICE_PROVIDERS = "hotel_service_providers";

    private final DSLContext dsl;

    public ServiceHealthResponse getServiceHealth() {
        try {
            return getServiceHealthInternal();
        } catch (Throwable ex) {
            log.warn("getServiceHealth failed: {}", ex.getMessage(), ex);
            return ServiceHealthResponse.builder()
                    .serviceCountByType(new HashMap<>())
                    .activeBookingCountByType(new HashMap<>())
                    .build();
        }
    }

    private ServiceHealthResponse getServiceHealthInternal() {
        var servicesTable = DSL.table(DSL.name(SERVICES));
        var bookingsTable = DSL.table(DSL.name(BOOKINGS));

        Field<String> sType = DSL.field(DSL.name(SERVICES, "type"), String.class);
        var sDeletedAtField = DSL.field(DSL.name(SERVICES, "deleted_at"));

        Map<String, Long> serviceCountByType = new HashMap<>();
        try {
            var rows = dsl.select(sType, DSL.count().as("cnt"))
                    .from(servicesTable)
                    .where(sDeletedAtField.isNull())
                    .groupBy(sType)
                    .fetch();
            for (var r : rows) {
                String key = r.get(sType) != null ? String.valueOf(r.get(sType)) : "UNKNOWN";
                Long val = r.get("cnt", Long.class);
                serviceCountByType.put(key, val != null ? val : 0L);
            }
        } catch (Throwable e) {
            log.debug("serviceCountByType query failed: {}", e.getMessage());
        }

        Map<String, Long> activeBookingCountByType = new HashMap<>();
        try {
            var bStatusSql = DSL.field(DSL.sql("(" + BOOKINGS + ".status::text)"), String.class);
            var rows = dsl.select(sType, DSL.count().as("cnt"))
                    .from(bookingsTable)
                    .join(servicesTable).on(DSL.field(DSL.name(BOOKINGS, "service_id")).eq(DSL.field(DSL.name(SERVICES, "id"))))
                    .where(bStatusSql.in("CONFIRMED", "ACTIVE"))
                    .and(sDeletedAtField.isNull())
                    .groupBy(sType)
                    .fetch();
            for (var r : rows) {
                String key = r.get(sType) != null ? String.valueOf(r.get(sType)) : "UNKNOWN";
                Long val = r.get("cnt", Long.class);
                activeBookingCountByType.put(key, val != null ? val : 0L);
            }
        } catch (Throwable e) {
            log.debug("activeBookingCountByType query failed: {}", e.getMessage());
        }

        return ServiceHealthResponse.builder()
                .serviceCountByType(serviceCountByType)
                .activeBookingCountByType(activeBookingCountByType)
                .build();
    }

    public CommissionAnalysisResponse getCommissionAnalysis(LocalDate start, LocalDate end) {
        try {
            return getCommissionAnalysisInternal(start, end);
        } catch (Throwable ex) {
            log.warn("getCommissionAnalysis failed: {}", ex.getMessage(), ex);
            LocalDate s = start != null ? start : LocalDate.now().minusMonths(1);
            LocalDate endDate = end != null ? end : LocalDate.now();
            return CommissionAnalysisResponse.builder()
                    .start(s)
                    .end(endDate)
                    .totalBaseAmount(BigDecimal.ZERO)
                    .totalCommissionAmount(BigDecimal.ZERO)
                    .currency("USD")
                    .build();
        }
    }

    private CommissionAnalysisResponse getCommissionAnalysisInternal(LocalDate start, LocalDate end) {
        LocalDateTime from = (start != null ? start : LocalDate.now().minusMonths(1)).atStartOfDay();
        LocalDateTime to = (end != null ? end : LocalDate.now()).atTime(23, 59, 59);

        var table = DSL.table(DSL.name(BOOKINGS));
        var createdAt = DSL.field(DSL.name(BOOKINGS, "created_at"), LocalDateTime.class);
        var baseAmount = DSL.field(DSL.name(BOOKINGS, "base_amount"), BigDecimal.class);
        var commissionAmount = DSL.field(DSL.name(BOOKINGS, "commission_amount"), BigDecimal.class);

        var rec = dsl.select(
                        DSL.coalesce(DSL.sum(baseAmount), BigDecimal.ZERO).as("total_base"),
                        DSL.coalesce(DSL.sum(commissionAmount), BigDecimal.ZERO).as("total_commission")
                )
                .from(table)
                .where(createdAt.ge(from).and(createdAt.le(to)))
                .fetchOne();

        BigDecimal totalBase = rec != null && rec.get("total_base") != null ? rec.get("total_base", BigDecimal.class) : BigDecimal.ZERO;
        BigDecimal totalCommission = rec != null && rec.get("total_commission") != null ? rec.get("total_commission", BigDecimal.class) : BigDecimal.ZERO;
        if (totalBase == null) totalBase = BigDecimal.ZERO;
        if (totalCommission == null) totalCommission = BigDecimal.ZERO;

        return CommissionAnalysisResponse.builder()
                .start(from.toLocalDate())
                .end(to.toLocalDate())
                .totalBaseAmount(totalBase)
                .totalCommissionAmount(totalCommission)
                .currency("USD")
                .build();
    }

    public PayoutSummaryResponse getPayouts(LocalDate start, LocalDate end) {
        try {
            return getPayoutsInternal(start, end);
        } catch (Throwable ex) {
            log.warn("getPayouts failed: {}", ex.getMessage(), ex);
            LocalDate s = start != null ? start : LocalDate.now().minusMonths(1);
            LocalDate endDate = end != null ? end : LocalDate.now();
            return PayoutSummaryResponse.builder()
                    .start(s)
                    .end(endDate)
                    .payouts(List.of())
                    .build();
        }
    }

    private PayoutSummaryResponse getPayoutsInternal(LocalDate start, LocalDate end) {
        LocalDateTime from = (start != null ? start : LocalDate.now().minusMonths(1)).atStartOfDay();
        LocalDateTime to = (end != null ? end : LocalDate.now()).atTime(23, 59, 59);

        var providerId = DSL.field(DSL.name(SERVICE_PROVIDERS, "id"), UUID.class);
        var companyName = DSL.field(DSL.name(SERVICE_PROVIDERS, "company_name"), String.class);
        var totalAmount = DSL.field(DSL.name(BOOKINGS, "total_amount"), BigDecimal.class);
        var commissionAmount = DSL.field(DSL.name(BOOKINGS, "commission_amount"), BigDecimal.class);
        var currency = DSL.field(DSL.name(BOOKINGS, "currency"), String.class);
        var createdAt = DSL.field(DSL.name(BOOKINGS, "created_at"), LocalDateTime.class);
        var bStatusSql = DSL.field(DSL.sql("(" + BOOKINGS + ".status::text)"), String.class);

        List<PayoutSummaryResponse.PayoutItem> payouts = new ArrayList<>();
        var rows = dsl.select(
                        providerId,
                        companyName,
                        DSL.coalesce(DSL.sum(totalAmount).minus(DSL.sum(commissionAmount)), BigDecimal.ZERO).as("payout"),
                        currency
                )
                .from(DSL.table(DSL.name(BOOKINGS)))
                .join(DSL.table(DSL.name(SERVICES))).on(DSL.field(DSL.name(BOOKINGS, "service_id")).eq(DSL.field(DSL.name(SERVICES, "id"))))
                .join(DSL.table(DSL.name(SERVICE_PROVIDERS))).on(DSL.field(DSL.name(SERVICES, "provider_id")).eq(DSL.field(DSL.name(SERVICE_PROVIDERS, "id"))))
                .where(createdAt.ge(from).and(createdAt.le(to)))
                .and(bStatusSql.in("CONFIRMED", "ACTIVE", "COMPLETED"))
                .groupBy(providerId, companyName, currency)
                .having(DSL.coalesce(DSL.sum(totalAmount).minus(DSL.sum(commissionAmount)), BigDecimal.ZERO).gt(BigDecimal.ZERO))
                .fetch();

        for (var r : rows) {
            payouts.add(PayoutSummaryResponse.PayoutItem.builder()
                    .providerId(r.get(providerId))
                    .providerName(r.get(companyName) != null ? r.get(companyName) : "")
                    .amount(r.get("payout", BigDecimal.class) != null ? r.get("payout", BigDecimal.class) : BigDecimal.ZERO)
                    .currency(r.get(currency) != null ? r.get(currency) : "USD")
                    .build());
        }

        return PayoutSummaryResponse.builder()
                .start(from.toLocalDate())
                .end(to.toLocalDate())
                .payouts(payouts)
                .build();
    }
}
