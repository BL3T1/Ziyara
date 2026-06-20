package com.ziyara.backend.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class AsyncExecutorConfig {

    @Bean(name = "taskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }

    /**
     * Bounded pool for BCrypt verification. BCrypt is pure CPU work — it pins a carrier thread
     * and cannot yield, so virtual threads provide no benefit. Capping concurrency at 2× CPU
     * prevents BCrypt from saturating all carrier threads and starving I/O-bound request handlers.
     */
    @Bean(name = "bcryptExecutor")
    public Executor bcryptExecutor() {
        int threads = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threads);
        executor.setMaxPoolSize(threads);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("bcrypt-");
        executor.initialize();
        return executor;
    }

    /**
     * Dedicated pool for async login audit writes (lastLoginAt, failed-attempts reset).
     * Kept separate from taskExecutor so login recording never blocks general async work.
     */
    @Bean(name = "loginAuditExecutor")
    public TaskExecutor loginAuditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("login-audit-");
        executor.initialize();
        return executor;
    }
}
