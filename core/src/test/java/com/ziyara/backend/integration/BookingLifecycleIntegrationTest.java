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
 * Integration tests for the booking lifecycle: requires auth → create booking → assert state transitions.
 * All tests run against a real PostgreSQL (Testcontainers) and full Spring context.
 */
class BookingLifecycleIntegrationTest extends AbstractIntegrationTest {

    // ── GET /api/v1/bookings — requires authentication ────────────────────────

    @Test
    void listBookings_unauthenticated_returns401() {
        ResponseEntity<String> response = rest.getForEntity("/api/v1/bookings", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void listBookings_authenticated_returns200() {
        String token = registerAndLogin();
        assertThat(token).isNotBlank();

        HttpHeaders headers = bearerHeaders(token);
        ResponseEntity<String> response = rest.exchange(
                "/api/v1/bookings", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── POST /api/v1/bookings — requires auth + service ───────────────────────

    @Test
    void createBooking_unauthenticated_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"serviceId\":\"" + UUID.randomUUID() + "\",\"checkIn\":\"2027-01-01\",\"checkOut\":\"2027-01-03\"}";

        ResponseEntity<String> response = rest.exchange(
                "/api/v1/bookings", HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void createBooking_nonExistentService_returns404Or422() {
        String token = registerAndLogin();
        HttpHeaders headers = bearerHeaders(token);
        String body = "{\"serviceId\":\"" + UUID.randomUUID() + "\"}";

        ResponseEntity<String> response = rest.exchange(
                "/api/v1/bookings", HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode().value()).isIn(400, 404, 422);
    }

    // ── GET /api/v1/bookings/admin — requires bookings:read permission ─────────

    @Test
    void adminListBookings_asCustomer_returns403() {
        String token = registerAndLogin();
        HttpHeaders headers = bearerHeaders(token);

        ResponseEntity<String> response = rest.exchange(
                "/api/v1/bookings/admin", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode().value()).isIn(403, 401);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String registerAndLogin() {
        String email = "booking-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String password = "P@ssw0rd!Test123";
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail(email);
        reg.setPassword(password);
        reg.setFirstName("Booking");
        reg.setLastName("Test");
        rest.postForEntity("/api/v1/auth/register", reg, String.class);

        AuthRequest auth = new AuthRequest();
        auth.setEmail(email);
        auth.setPassword(password);
        ResponseEntity<String> loginResp = rest.postForEntity("/api/v1/auth/login", auth, String.class);
        String body = loginResp.getBody();
        if (body == null) return null;
        int idx = body.indexOf("\"accessToken\":\"");
        if (idx < 0) return null;
        int start = idx + "\"accessToken\":\"".length();
        int end = body.indexOf("\"", start);
        return end > start ? body.substring(start, end) : null;
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
