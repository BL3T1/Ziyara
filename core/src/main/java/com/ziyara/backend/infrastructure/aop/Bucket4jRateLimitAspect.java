package com.ziyara.backend.infrastructure.aop;

import com.ziyara.backend.application.annotation.RateLimit;
import com.ziyara.backend.application.exception.RateLimitedException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

/**
 * Rate-limiting AOP using Bucket4j with a Redis-backed distributed proxy manager.
 * Buckets are stored in Redis so limits are enforced globally across all app instances.
 * Uses greedy refill (continuous token addition) rather than a tumbling window to
 * prevent the double-burst problem at window boundaries.
 * Fail-open: if Redis is unavailable, the request is allowed through with a warning log.
 */
@Aspect
@Component
@Slf4j
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class Bucket4jRateLimitAspect {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    private ProxyManager<String> proxyManager;

    @PostConstruct
    void init() {
        LettuceConnectionFactory lcf = (LettuceConnectionFactory) redisConnectionFactory;
        RedisClient redisClient = (RedisClient) lcf.getNativeClient();
        StatefulRedisConnection<String, byte[]> conn =
                redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
        proxyManager = LettuceBasedProxyManager.builderFor(conn)
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy.fixedTimeToLive(Duration.ofMinutes(2)))
                .build();
        log.info("Bucket4j rate limiting active (Redis-backed, distributed)");
    }

    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String ip = extractIp();
        String endpointKey = rateLimit.key().isEmpty()
                ? pjp.getSignature().toShortString()
                : rateLimit.key();
        String bucketKey = "ziyara:rl:" + ip + ":" + endpointKey;

        try {
            boolean consumed = proxyManager.builder()
                    .build(bucketKey, () -> buildBucketConfig(rateLimit.maxPerMinute()))
                    .tryConsume(1);
            if (!consumed) {
                log.warn("Rate limit exceeded: ip={} endpoint={}", ip, endpointKey);
                throw new RateLimitedException("Too many requests. Please try again later.");
            }
        } catch (RateLimitedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Rate limit Redis check failed (fail-open, request allowed): {}", e.getMessage());
        }

        return pjp.proceed();
    }

    private static BucketConfiguration buildBucketConfig(int maxPerMinute) {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(maxPerMinute)
                        .refillGreedy(maxPerMinute, Duration.ofMinutes(1))
                        .initialTokens(maxPerMinute)
                        .build())
                .build();
    }

    private static String extractIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "unknown";
            HttpServletRequest request = attrs.getRequest();
            String xfHeader = request.getHeader("X-Forwarded-For");
            if (xfHeader != null && !xfHeader.isBlank()) {
                return xfHeader.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (RuntimeException e) {
            return "unknown";
        }
    }
}
