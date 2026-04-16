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
        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(8);
        ex.setQueueCapacity(100);
        ex.setThreadNamePrefix("dashboard-");
        ex.initialize();
        return ex;
    }
}
