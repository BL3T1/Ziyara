package com.ziyara.backend.domain.repository;

import java.time.Instant;

/**
 * Repository Port: RateLimitRepository
 * Persists rate-limit counters for the Postgres fallback path.
 */
public interface RateLimitRepository {

    /**
     * Atomically increments (or inserts) the counter for the given window and returns the new count.
     * Returns null on transient DB failure (caller should fail open).
     */
    Integer incrementAndGet(String identifier, String identifierType, String endpoint,
                            Instant windowStart, Instant windowEnd);

    /** Deletes counters whose window has expired beyond the retention period. */
    void deleteExpired();
}
