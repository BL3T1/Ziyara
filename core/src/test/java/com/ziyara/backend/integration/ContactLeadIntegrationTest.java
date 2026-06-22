package com.ziyara.backend.integration;

import com.ziyara.backend.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for POST /api/v1/public/contact — the public contact-lead submission endpoint.
 * Tests: successful submission, validation errors, and per-email rate limiting (60 s cooldown).
 */
class ContactLeadIntegrationTest extends AbstractIntegrationTest {

    private static final String ENDPOINT = "/api/v1/public/contact";

    // ── successful submission ─────────────────────────────────────────────────

    @Test
    void contact_validRequest_returns200() {
        ResponseEntity<String> response = post(uniqueEmail(), "Test User", "Test Company", "Hello, I am interested in your services.");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("received your message");
    }

    // ── validation ────────────────────────────────────────────────────────────

    @Test
    void contact_missingEmail_returns400() {
        String body = """
                {"name":"Test","message":"hello"}
                """;
        ResponseEntity<String> response = rest.postForEntity(ENDPOINT, jsonEntity(body), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void contact_invalidEmail_returns400() {
        String body = """
                {"name":"Test","email":"not-an-email","message":"hello"}
                """;
        ResponseEntity<String> response = rest.postForEntity(ENDPOINT, jsonEntity(body), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void contact_missingMessage_returns400() {
        String body = """
                {"name":"Test","email":"test@example.com"}
                """;
        ResponseEntity<String> response = rest.postForEntity(ENDPOINT, jsonEntity(body), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── per-email rate limit ──────────────────────────────────────────────────

    @Test
    void contact_sameEmailTwiceWithinCooldown_returns429() {
        String email = uniqueEmail();

        // First submission succeeds
        ResponseEntity<String> first = post(email, "User A", null, "I would like to enquire about your packages.");
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Second submission with the same email within the 60 s cooldown is rate-limited.
        // The service checks per email+IP; in tests the IP resolves to 127.0.0.1.
        ResponseEntity<String> second = post(email, "User A", null, "Following up on my previous message.");
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void contact_differentEmails_bothSucceed() {
        ResponseEntity<String> r1 = post(uniqueEmail(), "User One", null, "First user asking about services.");
        ResponseEntity<String> r2 = post(uniqueEmail(), "User Two", null, "Second user asking about pricing.");

        assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r2.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ResponseEntity<String> post(String email, String name, String company, String message) {
        String companyPart = company != null ? "\"company\":\"" + company + "\"," : "";
        String body = ("{\"name\":\"%s\",\"email\":\"%s\",%s\"message\":\"%s\"}")
                .formatted(name, email, companyPart, message);
        return rest.postForEntity(ENDPOINT, jsonEntity(body), String.class);
    }

    private HttpEntity<String> jsonEntity(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private String uniqueEmail() {
        return "lead-" + System.nanoTime() + "@test.com";
    }
}
