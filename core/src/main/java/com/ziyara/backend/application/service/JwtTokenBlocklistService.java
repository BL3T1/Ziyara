package com.ziyara.backend.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Revoked JWT {@code jti} until natural expiry. Prefers Redis when available; otherwise in-memory (single node).
 */
@Service
@Slf4j
public class JwtTokenBlocklistService {

    private static final String REDIS_PREFIX = "ziyara:jwt:revoked:";

    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<String, Long> memoryExpiryEpochSeconds = new ConcurrentHashMap<>();

    public JwtTokenBlocklistService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void revokeUntilExpiry(String jti, Instant expiresAt) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        long now = Instant.now().getEpochSecond();
        long exp = expiresAt.getEpochSecond();
        long ttl = Math.max(1L, exp - now);
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(REDIS_PREFIX + jti, "1", Duration.ofSeconds(ttl));
                return;
            } catch (Exception e) {
                log.warn("Redis revoke failed; falling back to memory: {}", e.getMessage());
            }
        }
        memoryExpiryEpochSeconds.put(jti, exp);
    }

    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        if (redisTemplate != null) {
            try {
                Boolean has = redisTemplate.hasKey(REDIS_PREFIX + jti);
                if (Boolean.TRUE.equals(has)) {
                    return true;
                }
            } catch (Exception e) {
                log.debug("Redis blocklist check failed: {}", e.getMessage());
            }
        }
        Long exp = memoryExpiryEpochSeconds.get(jti);
        if (exp == null) {
            return false;
        }
        if (Instant.now().getEpochSecond() > exp) {
            memoryExpiryEpochSeconds.remove(jti);
            return false;
        }
        return true;
    }
}
