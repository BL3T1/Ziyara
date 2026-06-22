package com.ziyara.backend.integration;

import com.ziyara.backend.AbstractIntegrationTest;
import com.ziyara.backend.application.dto.AuthRequest;
import com.ziyara.backend.application.dto.request.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the payment API.
 * Covers security guards, Bean Validation, and not-found paths.
 * Payment gateway side-effects are not tested here (no mock gateway container).
 */
class PaymentIntegrationTest extends AbstractIntegrationTest {

    // ── POST /payments — authentication guard ──────────────────────────────────

    @Test
    void initiatePayment_unauthenticated_returns401() {
        HttpHeaders headers = jsonHeaders(null);
        String body = "{\"bookingId\":\"" + UUID.randomUUID() + "\","
                + "\"amount\":100.00,\"currency\":\"USD\",\"method\":\"CREDIT_CARD\"}";

        ResponseEntity<String> response = rest.exchange(
                "/api/v1/payments", HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void initiatePaymentAlias_unauthenticated_returns401() {
        HttpHeaders headers = jsonHeaders(null);
        String body = "{\"bookingId\":\"" + UUID.randomUUID() + "\","
                + "\"amount\":100.00,\"currency\":\"USD\",\"method\":\"CREDIT_CARD\"}";

        ResponseEntity<String> response = rest.exchange(
                "/api/v1/payments/initiate", HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── POST /payments — validation ────────────────────────────────────────────

    @Test
    void initiatePayment_missingBookingId_returns400() {
        String token = registerAndLogin();

        HttpHeaders headers = jsonHeaders(token);
        // bookingId is missing (null)
        String body = "{\"amount\":100.00,\"currency\":\"USD\",\"method\":\"CREDIT_CARD\"}";

        ResponseEntity<String> response = rest.exchange(
                "/api/v1/payments", HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void initiatePayment_missingAmount_returns400() {
        String token = registerAndLogin();

        HttpHeaders headers = jsonHeaders(token);
        String body = "{\"bookingId\":\"" + UUID.randomUUID() + "\","
                + "\"currency\":\"USD\",\"method\":\"CREDIT_CARD\"}";

        ResponseEntity<String> response = rest.exchange(
                "/api/v1/payments", HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void initiatePayment_zeroAmount_returns400() {
        String token = registerAndLogin();

        HttpHeaders headers = jsonHeaders(token);
        String body = "{\"bookingId\":\"" + UUID.randomUUID() + "\","
                + "\"amount\":0.00,\"currency\":\"USD\",\"method\":\"CREDIT_CARD\"}";

        ResponseEntity<String> response = rest.exchange(
                "/api/v1/payments", HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── POST /payments — non-existent booking ─────────────────────────────────

    @Test
    void initiatePayment_nonExistentBooking_returns4xx() {
        String token = registerAndLogin();

        HttpHeaders headers = jsonHeaders(token);
        String body = "{\"bookingId\":\"" + UUID.randomUUID() + "\","
                + "\"amount\":150.00,\"currency\":\"USD\",\"method\":\"CREDIT_CARD\"}";

        ResponseEntity<String> response = rest.exchange(
                "/api/v1/payments", HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);

        // Booking not found → 404 or unprocessable
        assertThat(response.getStatusCode().value()).isIn(404, 422, 400);
    }

    // ── GET /payments — requires payments:read permission ─────────────────────

    @Test
    void listPayments_unauthenticated_returns401() {
        ResponseEntity<String> response = rest.getForEntity("/api/v1/payments", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void listPayments_customerWithoutPermission_returns403() {
        // A freshly registered customer has no payments:read permission
        String token = registerAndLogin();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> response = rest.exchange(
                "/api/v1/payments", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void paymentSummary_unauthenticated_returns401() {
        ResponseEntity<String> response = rest.getForEntity("/api/v1/payments/summary", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void paymentSummary_customerWithoutPermission_returns403() {
        String token = registerAndLogin();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> response = rest.exchange(
                "/api/v1/payments/summary", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String registerAndLogin() {
        String email = "pay-" + shortId() + "@test.com";
        String password = "P@ssw0rd!Test123";

        RegisterRequest reg = new RegisterRequest();
        reg.setEmail(email);
        reg.setPassword(password);
        reg.setFirstName("Pay");
        reg.setLastName("Tester");
        rest.postForEntity("/api/v1/auth/register", reg, String.class);

        AuthRequest auth = new AuthRequest();
        auth.setEmail(email);
        auth.setPassword(password);
        ResponseEntity<String> loginResp = rest.postForEntity("/api/v1/auth/login", auth, String.class);
        return extractToken(loginResp.getBody());
    }

    private HttpHeaders jsonHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
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
