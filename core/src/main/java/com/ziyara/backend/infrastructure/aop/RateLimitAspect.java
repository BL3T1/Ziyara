package com.ziyara.backend.infrastructure.aop;

import com.ziyara.backend.application.annotation.RateLimit;
import com.ziyara.backend.application.exception.RateLimitedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Aspect
@Component
@Slf4j
public class RateLimitAspect {

    private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String ip = extractIp();
        String endpointKey = rateLimit.key().isEmpty()
                ? pjp.getSignature().toShortString()
                : rateLimit.key();
        long windowMinute = Instant.now().truncatedTo(ChronoUnit.MINUTES).toEpochMilli() / 60_000;
        String cacheKey = ip + ":" + endpointKey + ":" + windowMinute;

        AtomicInteger counter = counters.computeIfAbsent(cacheKey, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();

        if (counters.size() > 50_000) {
            long currentWindow = windowMinute;
            counters.keySet().removeIf(k -> {
                String[] parts = k.split(":");
                if (parts.length < 1) return true;
                try {
                    return Long.parseLong(parts[parts.length - 1]) < currentWindow;
                } catch (NumberFormatException e) {
                    return true;
                }
            });
        }

        if (count > rateLimit.maxPerMinute()) {
            log.warn("Rate limit exceeded: ip={} endpoint={} count={}", ip, endpointKey, count);
            throw new RateLimitedException("Too many requests. Please try again later.");
        }
        return pjp.proceed();
    }

    private static String extractIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return "unknown";
            }
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
