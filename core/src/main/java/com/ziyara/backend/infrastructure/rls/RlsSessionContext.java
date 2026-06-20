package com.ziyara.backend.infrastructure.rls;

/**
 * Thread-local RLS attributes applied on every pooled {@link javax.sql.DataSource#getConnection()}.
 */
public final class RlsSessionContext {

    private static final ThreadLocal<RlsSessionAttributes> HOLDER = new ThreadLocal<>();

    private RlsSessionContext() {
    }

    public static void set(RlsSessionAttributes attributes) {
        HOLDER.set(attributes);
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static RlsSessionAttributes getOrDefault() {
        RlsSessionAttributes a = HOLDER.get();
        return a != null ? a : RlsSessionAttributes.open();
    }
}
