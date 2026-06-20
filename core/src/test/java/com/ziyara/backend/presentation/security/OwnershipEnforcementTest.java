package com.ziyara.backend.presentation.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziyara.core.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Security integration tests: verifies that authenticated users cannot access resources
 * owned by other users (IDOR prevention). Requires Docker (Testcontainers).
 *
 * Runs with the real SecurityConfig + JWT authentication (no mocking).
 */
@Tag("docker")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class OwnershipEnforcementTest {

    @LocalServerPort
    private int port;

    private String baseUrl;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1";
    }

    // ── User profile IDOR ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Customer B cannot read Customer A's profile by ID → 403")
    void getUserById_crossUserCustomer_returns403() throws Exception {
        // Register + login user A
        String emailA = "idor-a-" + System.nanoTime() + "@test.example";
        registerCustomer(emailA);
        String tokenA = login(emailA);
        String userAId = extractUserId(tokenA);

        // Register + login user B
        String emailB = "idor-b-" + System.nanoTime() + "@test.example";
        registerCustomer(emailB);
        String tokenB = login(emailB);

        // B tries to access A's profile
        try {
            restTemplate.exchange(
                    baseUrl + "/users/" + userAId,
                    HttpMethod.GET,
                    new HttpEntity<>(bearerHeaders(tokenB)),
                    String.class
            );
            throw new AssertionError("Expected 403 but request succeeded");
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    @DisplayName("Customer A can read their own profile by ID → 200")
    void getUserById_selfAccess_returns200() throws Exception {
        String email = "idor-self-" + System.nanoTime() + "@test.example";
        registerCustomer(email);
        String token = login(email);
        String userId = extractUserId(token);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/users/" + userId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> body = objectMapper.readValue(response.getBody(), Map.class);
        assertThat(body.get("success")).isEqualTo(true);
    }

    @Test
    @DisplayName("Unauthenticated access to /users/me → 401")
    void usersMe_noToken_returns401() {
        try {
            restTemplate.getForEntity(baseUrl + "/users/me", String.class);
            throw new AssertionError("Expected 401");
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    @DisplayName("Customer A can access /users/me → 200 with own email")
    void usersMe_authenticated_returnsOwnProfile() throws Exception {
        String email = "idor-me-" + System.nanoTime() + "@test.example";
        registerCustomer(email);
        String token = login(email);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/users/me",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> body = objectMapper.readValue(response.getBody(), Map.class);
        Map<?, ?> data = (Map<?, ?>) body.get("data");
        assertThat(data.get("email")).isEqualTo(email);
    }

    @Test
    @DisplayName("Customer cannot access privileged /users list → 403")
    void listUsers_customerRole_returns403() throws Exception {
        String email = "idor-list-" + System.nanoTime() + "@test.example";
        registerCustomer(email);
        String token = login(email);

        try {
            restTemplate.exchange(
                    baseUrl + "/users",
                    HttpMethod.GET,
                    new HttpEntity<>(bearerHeaders(token)),
                    String.class
            );
            throw new AssertionError("Expected 403");
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void registerCustomer(String email) {
        String body = """
                {"email":"%s","password":"Customer123!","role":"CUSTOMER","currency":"USD"}
                """.formatted(email);
        assertThatCode(() -> restTemplate.postForEntity(
                baseUrl + "/auth/register",
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        )).doesNotThrowAnyException();
    }

    @SuppressWarnings("unchecked")
    private String login(String email) throws Exception {
        String body = """
                {"email":"%s","password":"Customer123!"}
                """.formatted(email);
        ResponseEntity<String> resp = restTemplate.postForEntity(
                baseUrl + "/auth/login",
                new HttpEntity<>(body, jsonHeaders()),
                String.class
        );
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> parsed = objectMapper.readValue(resp.getBody(), Map.class);
        Map<String, Object> data = (Map<String, Object>) parsed.get("data");
        return (String) data.get("accessToken");
    }

    @SuppressWarnings("unchecked")
    private String extractUserId(String accessToken) throws Exception {
        ResponseEntity<String> resp = restTemplate.exchange(
                baseUrl + "/users/me",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(accessToken)),
                String.class
        );
        Map<String, Object> body = objectMapper.readValue(resp.getBody(), Map.class);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        return (String) data.get("id");
    }

    private static HttpHeaders bearerHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return h;
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }
}
