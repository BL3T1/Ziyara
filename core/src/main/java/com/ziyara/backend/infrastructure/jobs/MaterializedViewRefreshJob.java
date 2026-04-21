package com.ziyara.backend.infrastructure.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Refreshes reporting materialized views (see Flyway {@code mv_pay_daily_totals}).
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "ziyara.reporting", name = "materialized-view-refresh-enabled", havingValue = "true")
public class MaterializedViewRefreshJob {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(cron = "${ziyara.reporting.materialized-view-cron:0 20 2 * * *}")
    public void refreshPaymentDailyTotals() {
        try {
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_pay_daily_totals");
            log.info("Refreshed materialized view mv_pay_daily_totals");
        } catch (Exception e) {
            log.warn("Materialized view refresh failed: {}", e.getMessage());
        }
    }
}
