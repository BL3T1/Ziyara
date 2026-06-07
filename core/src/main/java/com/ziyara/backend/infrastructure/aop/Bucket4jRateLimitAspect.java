package com.ziyara.backend.infrastructure.aop;

import com.ziyara.backend.application.annotation.RateLimit;
import com.ziyara.backend.application.exception.RateLimitedException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate-limiting AOP using Bucket4j token-bucket algorithm.
 * One bucket per (IP + endpoint key); capacity = maxPerMinute tokens,
 * refilled every 60 seconds (tumbling window matches the original behaviour).
 */
@Aspect
@Component
@Slf4j
public class Bucket4jRateLimitAspect {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String ip = extractIp();
        String endpointKey = rateLimit.key().isEmpty()
                ? pjp.getSignature().toShortString()
                : rateLimit.key();
        String bucketKey = ip + ":" + endpointKey;

        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> buildBucket(rateLimit.maxPerMinute()));

        if (!bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded: ip={} endpoint={}", ip, endpointKey);
            throw new RateLimitedException("Too many requests. Please try again later.");
        }
        return pjp.proceed();
    }

    private static Bucket buildBucket(int maxPerMinute) {
        Bandwidth limit = Bandwidth.classic(maxPerMinute,
                Refill.intervally(maxPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
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
