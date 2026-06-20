package com.ziyara.backend.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Bounded pool for parallel dashboard read queries (KPIs, bootstrap aggregation).
 */
@Configuration
public class DashboardExecutorConfig {

    public static final String DASHBOARD_EXECUTOR = "dashboardExecutor";

    @Bean(name = DASHBOARD_EXECUTOR)
    public Executor dashboardExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        // Under stress load 30 VUs × 5 futures = 150 concurrent tasks.
        // With Redis caching, cached futures complete in ~1ms, so 20 threads clears
        // the burst quickly. Queue=200 prevents RejectedExecutionException overflow.
        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(20);
        ex.setQueueCapacity(200);
        ex.setThreadNamePrefix("dashboard-");
        ex.initialize();
        return ex;
    }
}
