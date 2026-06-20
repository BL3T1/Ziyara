package com.ziyara.backend.integration;

import com.ziyara.backend.AbstractIntegrationTest;
import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.AuthRequest;
import com.ziyara.backend.application.dto.AuthResponse;
import com.ziyara.backend.application.dto.request.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the auth lifecycle against a real Testcontainers PostgreSQL database.
 * Tests: register, login, logout, refresh, and error paths.
 */
class AuthIntegrationTest extends AbstractIntegrationTest {

    // ── POST /api/v1/auth/register ────────────────────────────────────────────

    @Test
    void register_validRequest_returns201() {
        RegisterRequest request = buildRegisterRequest("register-" + uuid() + "@test.com", "P@ssw0rd!Test123");

        ResponseEntity<String> response = rest.postForEntity("/api/v1/auth/register", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void register_duplicateEmail_returns409OrBadRequest() {
        String email = "dup-" + uuid() + "@test.com";
        RegisterRequest request = buildRegisterRequest(email, "P@ssw0rd!Test123");

        rest.postForEntity("/api/v1/auth/register", request, String.class);
        ResponseEntity<String> second = rest.postForEntity("/api/v1/auth/register", request, String.class);

        assertThat(second.getStatusCode().value()).isIn(400, 409);
    }

    @Test
    void register_invalidEmail_returns400() {
        RegisterRequest request = buildRegisterRequest("not-an-email", "P@ssw0rd!Test123");

        ResponseEntity<String> response = rest.postForEntity("/api/v1/auth/register", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── POST /api/v1/auth/login ───────────────────────────────────────────────

    @Test
    void login_validCredentials_returns200WithToken() {
        String email = "login-" + uuid() + "@test.com";
        String password = "P@ssw0rd!Test123";
        rest.postForEntity("/api/v1/auth/register", buildRegisterRequest(email, password), String.class);

        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail(email);
        authRequest.setPassword(password);

        ResponseEntity<String> response = rest.postForEntity("/api/v1/auth/login", authRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("accessToken");
    }

    @Test
    void login_wrongPassword_returns401() {
        String email = "wrong-pw-" + uuid() + "@test.com";
        rest.postForEntity("/api/v1/auth/register", buildRegisterRequest(email, "P@ssw0rd!Test123"), String.class);

        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail(email);
        authRequest.setPassword("WrongPassword!");

        ResponseEntity<String> response = rest.postForEntity("/api/v1/auth/login", authRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_nonExistentUser_returns401() {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail("ghost-" + uuid() + "@test.com");
        authRequest.setPassword("Password123!");

        ResponseEntity<String> response = rest.postForEntity("/api/v1/auth/login", authRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── POST /api/v1/auth/logout ──────────────────────────────────────────────

    @Test
    void logout_withValidToken_returns200() {
        String token = registerAndLogin();
        assertThat(token).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = rest.exchange(
                "/api/v1/auth/logout", HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── POST /api/v1/auth/refresh ─────────────────────────────────────────────

    @Test
    void refresh_withoutToken_returns401() {
        ResponseEntity<String> response = rest.postForEntity("/api/v1/auth/refresh", null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refresh_withInvalidToken_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Refresh-Token", "invalid.jwt.token");
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = rest.exchange(
                "/api/v1/auth/refresh", HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String registerAndLogin() {
        String email = "flow-" + uuid() + "@test.com";
        String password = "P@ssw0rd!Test123";
        rest.postForEntity("/api/v1/auth/register", buildRegisterRequest(email, password), String.class);

        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail(email);
        authRequest.setPassword(password);

        ResponseEntity<String> loginResp = rest.postForEntity("/api/v1/auth/login", authRequest, String.class);
        String body = loginResp.getBody();
        if (body == null) return null;

        int idx = body.indexOf("\"accessToken\":\"");
        if (idx < 0) return null;
        int start = idx + "\"accessToken\":\"".length();
        int end = body.indexOf("\"", start);
        return end > start ? body.substring(start, end) : null;
    }

    private RegisterRequest buildRegisterRequest(String email, String password) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword(password);
        req.setFirstName("Test");
        req.setLastName("User");
        return req;
    }

    private String uuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
