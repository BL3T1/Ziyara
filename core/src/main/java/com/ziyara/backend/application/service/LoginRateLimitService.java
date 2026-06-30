package com.ziyara.backend.application.service;

import com.ziyara.backend.domain.repository.RateLimitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import org.springframework.scheduling.annotation.Scheduled;

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

    private final RateLimitRepository rateLimitRepository;
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
        Instant windowStart = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        Instant windowEnd = windowStart.plus(1, ChronoUnit.MINUTES);
        // Fail open: null return means transient DB failure
        Integer cnt = rateLimitRepository.incrementAndGet("ip:" + clientIp, "IP", endpointKey, windowStart, windowEnd);
        return cnt == null || cnt <= maxPerMinutePerIp;
    }

    // Cleanup moved out of the hot path — runs every 10 minutes in the background.
    @Scheduled(fixedDelay = 600_000)
    public void cleanupExpiredCounters() {
        rateLimitRepository.deleteExpired();
    }
}
