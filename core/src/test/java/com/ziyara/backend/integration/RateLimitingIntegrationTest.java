package com.ziyara.backend.integration;

import com.ziyara.backend.AbstractIntegrationTest;
import com.ziyara.backend.application.dto.AuthRequest;
import com.ziyara.backend.application.dto.request.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for login rate limiting via the Postgres-backed
 * {@link com.ziyara.backend.application.service.LoginRateLimitService}.
 *
 * The base test profile disables rate limiting; this class re-enables it and
 * sets the threshold to 3 attempts per minute so tests don't need 40 attempts.
 * Redis is still excluded — the service automatically falls back to
 * {@code sys_rate_limit_counters} (Postgres).
 */
@TestPropertySource(properties = {
        "ziyara.rate-limit.login.enabled=true",
        "ziyara.rate-limit.login.redis-enabled=false",
        "ziyara.rate-limit.login.max-per-minute-per-ip=3"
})
class RateLimitingIntegrationTest extends AbstractIntegrationTest {

    // ── under threshold ───────────────────────────────────────────────────────

    @Test
    void loginAttempt_underThreshold_returnsNot429() {
        AuthRequest req = badCredentials();

        // First 3 attempts: rate limiter permits them (all wrong password → 401)
        for (int i = 0; i < 3; i++) {
            ResponseEntity<String> resp = rest.postForEntity("/api/v1/auth/login", req, String.class);
            assertThat(resp.getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    // ── over threshold ────────────────────────────────────────────────────────

    @Test
    void loginAttempt_exceedsThreshold_returns429() {
        AuthRequest req = badCredentials();

        // Exhaust the 3-attempt window
        for (int i = 0; i < 3; i++) {
            rest.postForEntity("/api/v1/auth/login", req, String.class);
        }

        // 4th attempt must be rate-limited
        ResponseEntity<String> rateLimited = rest.postForEntity("/api/v1/auth/login", req, String.class);
        assertThat(rateLimited.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    // ── valid login always passes (rate limit on failed attempts only) ─────────

    @Test
    void validLogin_notRateLimited() {
        String email = "rateok-" + shortId() + "@test.com";
        String password = "P@ssw0rd!Test123";
        register(email, password);

        AuthRequest req = new AuthRequest();
        req.setEmail(email);
        req.setPassword(password);

        // Three successful logins must all return 200, not 429
        for (int i = 0; i < 3; i++) {
            ResponseEntity<String> resp = rest.postForEntity("/api/v1/auth/login", req, String.class);
            assertThat(resp.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.CREATED);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private AuthRequest badCredentials() {
        AuthRequest req = new AuthRequest();
        req.setEmail("ghost-" + shortId() + "@test.com");
        req.setPassword("WrongPassword!");
        return req;
    }

    private void register(String email, String password) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword(password);
        req.setFirstName("Rate");
        req.setLastName("Tester");
        rest.postForEntity("/api/v1/auth/register", req, String.class);
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
