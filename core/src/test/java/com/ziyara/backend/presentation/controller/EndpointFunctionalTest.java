package com.ziyara.backend.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziyara.core.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * True functional endpoint tests: real server, Testcontainers Postgres, Hibernate-created schema,
 * seeded super-admin. Exercises auth, /me, health, register, and a few protected endpoints.
 * Uses functest profile so production SecurityConfig is excluded and this test provides permissive
 * security chains plus PasswordEncoder and CorsConfigurationSource.
 */
@Tag("docker")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("functest")
@Import({TestcontainersConfiguration.class, EndpointFunctionalTest.FunctestSecurityConfig.class})
class EndpointFunctionalTest {

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebSecurity
    static class FunctestSecurityConfig {
        // CSRF disabled: test-only filter chains for RestTemplate against the local server (production uses real SecurityConfig).
        @Bean(name = "authFilterChain")
        @Order(1)
        SecurityFilterChain authFilterChain(HttpSecurity http) throws Exception {
            http.securityMatcher("/auth/**", "/actuator/**", "/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**")
                    .csrf(csrf -> csrf.disable())
                    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        @Bean(name = "securityFilterChain")
        @Order(2)
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.securityMatcher("/**")
                    .csrf(csrf -> csrf.disable())
                    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        CorsConfigurationSource corsConfigurationSource() {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(List.of("*"));
            configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
            configuration.setAllowedHeaders(List.of("*"));
            configuration.setExposedHeaders(List.of("Authorization"));
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", configuration);
            return source;
        }
    }

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1";
    }

    @Nested
    @DisplayName("Auth")
    class Auth {

        @Test
        @DisplayName("POST /auth/register then POST /auth/login returns 201 then 200 with tokens")
        void registerThenLogin_returns201Then200WithTokens() throws Exception {
            String email = "customer-ft-%s@ziyarah.com".formatted(System.currentTimeMillis());
            String password = System.getenv().getOrDefault("FUNCTEST_DEFAULT_PASSWORD", "Customer123!");
            String registerBody = """
                {"email":"%s","password":"%s","role":"CUSTOMER"}
                """.formatted(email, password);

            ResponseEntity<String> registerResponse = restTemplate.postForEntity(
                    baseUrl + "/auth/register",
                    new HttpEntity<>(registerBody, jsonHeaders()),
                    String.class
            );
            assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            String loginBody = """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
            ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                    baseUrl + "/auth/login",
                    new HttpEntity<>(loginBody, jsonHeaders()),
                    String.class
            );
            assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<?, ?> parsed = objectMapper.readValue(loginResponse.getBody(), Map.class);
            assertThat(parsed.get("success")).isEqualTo(true);
            Map<?, ?> data = (Map<?, ?>) parsed.get("data");
            assertThat(data).isNotNull();
            assertThat(data.get("accessToken")).isNotNull();
            assertThat(data.get("email")).isEqualTo(email);
        }

        @Test
        @DisplayName("POST /auth/login with wrong password returns 401")
        void login_withWrongPassword_returns401() {
            String email = "nonexistent-%s@ziyarah.com".formatted(System.currentTimeMillis());
            String body = """
                {"email":"%s","password":"WrongPassword1!"}
                """.formatted(email);

            try {
                restTemplate.postForEntity(
                        baseUrl + "/auth/login",
                        new HttpEntity<>(body, jsonHeaders()),
                        String.class
                );
            } catch (HttpClientErrorException e) {
                assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                return;
            }
            throw new AssertionError("Expected 401");
        }

        @Test
        @DisplayName("POST /auth/register with valid body returns 201")
        void register_withValidBody_returns201() {
            String body = """
                {"email":"customer-%s@ziyarah.com","password":"Customer123!","role":"CUSTOMER"}
                """.formatted(System.currentTimeMillis());

            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/auth/register",
                    new HttpEntity<>(body, jsonHeaders()),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }
    }

    @Nested
    @DisplayName("Protected endpoints with JWT")
    class ProtectedEndpoints {

        @Test
        @DisplayName("GET /users/me without token returns 401")
        void getMe_withoutToken_returns401() {
            try {
                restTemplate.getForEntity(baseUrl + "/users/me", String.class);
            } catch (HttpClientErrorException e) {
                assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                return;
            }
            throw new AssertionError("Expected 401");
        }

        @Test
        @DisplayName("GET /notifications is mapped and reachable (not 404)")
        void getNotifications_isReachable() {
            assertThat(getStatusCode(baseUrl + "/notifications").value()).isNotEqualTo(404);
        }

        @Test
        @DisplayName("GET /services is mapped and reachable (not 404)")
        void getServices_isReachable() {
            assertThat(getStatusCode(baseUrl + "/services").value()).isNotEqualTo(404);
        }

        @Test
        @DisplayName("GET /bookings is mapped and reachable (not 404)")
        void getBookings_isReachable() {
            assertThat(getStatusCode(baseUrl + "/bookings").value()).isNotEqualTo(404);
        }
    }

    private org.springframework.http.HttpStatusCode getStatusCode(String url) {
        try {
            return restTemplate.getForEntity(url, String.class).getStatusCode();
        } catch (HttpClientErrorException | org.springframework.web.client.HttpServerErrorException e) {
            return e.getStatusCode();
        }
    }

    @Nested
    @DisplayName("Public / actuator")
    class Public {

        @Test
        @DisplayName("GET /actuator/health returns 200")
        void actuatorHealth_returns200() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    baseUrl + "/actuator/health",
                    String.class
            );
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
