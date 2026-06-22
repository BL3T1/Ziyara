package com.ziyara.backend.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window idle timeout for JWT sessions.
 *
 * How it works:
 *   - Each authenticated request calls {@link #touchAndCheck(String)}.
 *   - First call with a new JTI: records the timestamp and returns true (allow).
 *   - Subsequent calls: if (now − lastSeen) > idleTimeoutMs → return false (idle-expired),
 *     otherwise refresh the timestamp and return true (allow).
 *   - Logout / revocation calls {@link #remove(String)} to release the entry.
 *
 * A scheduled job prunes entries that have been idle longer than the timeout so the
 * map does not grow without bound across long-running deployments.
 *
 * Trade-off: in-memory only — sessions survive a single JVM instance.  For a
 * horizontally-scaled deployment replace this with a Redis GETSET + EXPIRE pair.
 */
@Component
@Slf4j
public class JwtIdleTimeoutService {

    private final long idleTimeoutMs;
    // jti → last-seen epoch-millisecond
    private final ConcurrentHashMap<String, Long> lastSeen = new ConcurrentHashMap<>();

    public JwtIdleTimeoutService(
            @Value("${app.security.session-timeout-minutes:30}") int idleTimeoutMinutes) {
        this.idleTimeoutMs = (long) idleTimeoutMinutes * 60 * 1000;
        log.info("JWT idle timeout: {} minutes", idleTimeoutMinutes);
    }

    /**
     * Record activity for the given JTI and return whether the session is still active.
     *
     * @param jti JWT ID claim from the access token
     * @return true  — session is active (allow the request)
     *         false — session has been idle longer than the configured timeout (reject)
     */
    public boolean touchAndCheck(String jti) {
        long now = System.currentTimeMillis();
        Long prev = lastSeen.put(jti, now);

        // First time this JTI is seen — token was just issued; not idle
        if (prev == null) return true;

        boolean active = (now - prev) <= idleTimeoutMs;
        if (!active) {
            lastSeen.remove(jti);
            log.debug("JWT idle timeout: jti={}", jti);
        }
        return active;
    }

    /**
     * Remove the activity record on logout or token revocation.
     */
    public void remove(String jti) {
        lastSeen.remove(jti);
    }

    /** Prune entries that have gone idle so the map does not grow indefinitely. */
    @Scheduled(fixedDelay = 300_000, initialDelay = 300_000)
    public void evictExpired() {
        long cutoff = System.currentTimeMillis() - idleTimeoutMs;
        int before = lastSeen.size();
        lastSeen.entrySet().removeIf(e -> e.getValue() < cutoff);
        int removed = before - lastSeen.size();
        if (removed > 0) {
            log.debug("JwtIdleTimeoutService: evicted {} expired entries", removed);
        }
    }
}
