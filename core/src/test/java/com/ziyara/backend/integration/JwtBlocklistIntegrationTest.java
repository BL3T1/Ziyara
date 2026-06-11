package com.ziyara.backend.integration;

import com.ziyara.backend.AbstractIntegrationTest;
import com.ziyara.backend.application.dto.AuthRequest;
import com.ziyara.backend.application.dto.request.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the JWT revocation (blocklist) lifecycle.
 * The test profile excludes RedisAutoConfiguration — JwtTokenBlocklistService
 * automatically falls back to its ConcurrentHashMap in-memory store, so these
 * tests do not require a running Redis container.
 *
 * Coverage: logout adds token to blocklist; subsequent use returns 401;
 * a fresh token obtained after logout still works.
 */
class JwtBlocklistIntegrationTest extends AbstractIntegrationTest {

    // ── logout revokes the token ──────────────────────────────────────────────

    @Test
    void logout_revokedToken_returns401OnSubsequentRequest() {
        String token = registerAndLogin();
        assertThat(token).isNotBlank();

        // Verify the token works before logout
        ResponseEntity<String> before = rest.exchange(
                "/api/v1/users/me", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)), String.class);
        assertThat(before.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Perform logout
        ResponseEntity<String> logoutResp = rest.exchange(
                "/api/v1/auth/logout", HttpMethod.POST,
                new HttpEntity<>(bearerHeaders(token)), String.class);
        assertThat(logoutResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Same token must now be rejected
        ResponseEntity<String> after = rest.exchange(
                "/api/v1/users/me", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)), String.class);
        assertThat(after.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logout_newTokenAfterLogout_stillWorks() {
        String email = "relogin-" + shortId() + "@test.com";
        String password = "P@ssw0rd!Test123";
        register(email, password);

        String firstToken = login(email, password);
        assertThat(firstToken).isNotBlank();

        // Logout first token
        rest.exchange("/api/v1/auth/logout", HttpMethod.POST,
                new HttpEntity<>(bearerHeaders(firstToken)), String.class);

        // Login again — new token
        String secondToken = login(email, password);
        assertThat(secondToken).isNotBlank();
        assertThat(secondToken).isNotEqualTo(firstToken);

        // New token works
        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/users/me", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(secondToken)), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void accessProtectedEndpoint_withoutToken_returns401() {
        ResponseEntity<String> response = rest.getForEntity("/api/v1/users/me", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void accessProtectedEndpoint_withInvalidToken_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("not.a.valid.jwt.token");

        ResponseEntity<String> response = rest.exchange(
                "/api/v1/users/me", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String registerAndLogin() {
        String email = "blocklist-" + shortId() + "@test.com";
        String password = "P@ssw0rd!Test123";
        register(email, password);
        return login(email, password);
    }

    private void register(String email, String password) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword(password);
        req.setFirstName("Blocklist");
        req.setLastName("Tester");
        rest.postForEntity("/api/v1/auth/register", req, String.class);
    }

    private String login(String email, String password) {
        AuthRequest req = new AuthRequest();
        req.setEmail(email);
        req.setPassword(password);
        ResponseEntity<String> resp = rest.postForEntity("/api/v1/auth/login", req, String.class);
        return extractToken(resp.getBody());
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private String extractToken(String body) {
        if (body == null) return null;
        int idx = body.indexOf("\"accessToken\":\"");
        if (idx < 0) return null;
        int start = idx + "\"accessToken\":\"".length();
        int end = body.indexOf("\"", start);
        return end > start ? body.substring(start, end) : null;
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
