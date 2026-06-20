package com.ziyara.backend.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import org.springframework.scheduling.annotation.Scheduled;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Login rate limiting: prefers Redis sliding counters when configured; otherwise Postgres
 * {@code sys_rate_limit_counters}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginRateLimitService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectProvider<StringRedisTemplate> redisTemplate;

    @Value("${ziyara.rate-limit.login.enabled:true}")
    private boolean enabled;

    @Value("${ziyara.rate-limit.login.redis-enabled}")
    private boolean redisEnabled;

    @Value("${ziyara.rate-limit.login.max-per-minute-per-ip:40}")
    private int maxPerMinutePerIp;

    public boolean allow(String clientIp, String endpointKey) {
        if (!enabled) {
            return true;
        }
        if (clientIp == null || clientIp.isBlank()) {
            return true;
        }
        StringRedisTemplate redis = redisTemplate.getIfAvailable();
        if (redisEnabled && redis != null) {
            return allowRedis(redis, clientIp, endpointKey);
        }
        return allowPostgres(clientIp, endpointKey);
    }

    private boolean allowRedis(StringRedisTemplate redis, String clientIp, String endpointKey) {
        Instant windowStart = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        String key = "ziyara:login:rl:" + sanitize(clientIp) + ":" + sanitize(endpointKey) + ":" + windowStart.toEpochMilli();
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, Duration.ofMinutes(2));
        }
        return count == null || count <= maxPerMinutePerIp;
    }

    private static String sanitize(String raw) {
        return raw.replace(':', '_').replace('\n', '_').replace('\r', '_');
    }

    private boolean allowPostgres(String clientIp, String endpointKey) {
        try {
            Instant windowStart = Instant.now().truncatedTo(ChronoUnit.MINUTES);
            Instant windowEnd = windowStart.plus(1, ChronoUnit.MINUTES);
            String identifier = "ip:" + clientIp;
            // Single UPSERT+RETURNING replaces the previous DELETE + INSERT + SELECT (3 round-trips).
            Integer cnt = jdbcTemplate.queryForObject(
                    """
                    INSERT INTO sys_rate_limit_counters
                      (id, identifier, identifier_type, endpoint, request_count, window_start, window_end)
                    VALUES (gen_random_uuid(), ?, 'IP', ?, 1, ?, ?)
                    ON CONFLICT (identifier, identifier_type, endpoint, window_start)
                    DO UPDATE SET request_count = sys_rate_limit_counters.request_count + 1
                    RETURNING request_count
                    """,
                    Integer.class,
                    identifier, endpointKey, Timestamp.from(windowStart), Timestamp.from(windowEnd));
            return cnt == null || cnt <= maxPerMinutePerIp;
        } catch (DataAccessException e) {
            // Fail open: older DBs without sys_rate_limit_counters (033) must not block login entirely.
            log.warn("Login rate limit DB check skipped (allowing request): {}", e.getMessage());
            return true;
        }
    }

    // Cleanup moved out of the hot path — runs every 10 minutes in the background.
    @Scheduled(fixedDelay = 600_000)
    public void cleanupExpiredCounters() {
        try {
            jdbcTemplate.update(
                    "DELETE FROM sys_rate_limit_counters WHERE window_end < NOW() - INTERVAL '2 days'");
        } catch (DataAccessException e) {
            log.warn("Rate limit counter cleanup failed: {}", e.getMessage());
        }
    }
}
