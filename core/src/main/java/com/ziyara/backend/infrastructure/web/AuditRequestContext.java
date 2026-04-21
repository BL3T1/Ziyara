package com.ziyara.backend.infrastructure.web;

import java.util.UUID;

/**
 * Per-request audit metadata (populated by {@link CorrelationIdFilter}).
 */
public final class AuditRequestContext {

    public record Holder(
            String correlationId,
            String requestId,
            UUID sessionId,
            UUID providerId,
            UUID tenantId,
            Integer riskScore,
            Integer durationMs,
            String tags) {}

    private static final ThreadLocal<Holder> CURRENT = new ThreadLocal<>();

    private AuditRequestContext() {
    }

    public static void set(Holder holder) {
        CURRENT.set(holder);
    }

    public static Holder get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
