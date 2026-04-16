package com.ziyara.backend.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziyara.backend.domain.entity.Role;
import com.ziyara.backend.domain.enums.RoleLevel;
import com.ziyara.backend.domain.enums.RoleStatus;
import com.ziyara.backend.domain.repository.RoleRepository;
import com.ziyara.core.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * Create staff user (system enum or custom primary RBAC) and verify login + /users/me (docker / Testcontainers).
 */
@Tag("docker")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("functest")
@Import({TestcontainersConfiguration.class, EndpointFunctionalTest.FunctestSecurityConfig.class})
class UserCreateAndLoginFunctionalTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RoleRepository roleRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1";
    }

    @Test
    @DisplayName("POST /users with role ACCOUNTANT then POST /auth/login succeeds; GET /users/me returns same role")
    void createWithSystemRole_thenLogin_andMe_ok() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String email = "staff-acct-" + suffix + "@ziyarah.com";
        String password = "Pass123!";

        String createBody = """
                {"email":"%s","password":"%s","role":"ACCOUNTANT","status":"ACTIVE"}
                """.formatted(email, password);

        ResponseEntity<String> createResponse = restTemplate.postForEntity(
                baseUrl + "/users",
                new HttpEntity<>(createBody, jsonHeaders()),
                String.class
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map<?, ?> loginData = loginAndGetData(email, password);
        assertThat(loginData.get("accessToken")).isNotNull();
        assertThat(loginData.get("role")).isEqualTo("ACCOUNTANT");

        String token = (String) loginData.get("accessToken");
        var meResponse = restTemplate.exchange(
                baseUrl + "/users/me",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                String.class
        );
        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> meParsed = objectMapper.readValue(meResponse.getBody(), Map.class);
        assertThat(meParsed.get("success")).isEqualTo(true);
        Map<?, ?> meData = (Map<?, ?>) meParsed.get("data");
        assertThat(meData).isNotNull();
        assertThat(meData.get("role")).isEqualTo("ACCOUNTANT");
    }

    @Test
    @DisplayName("POST /users with primaryRbacRoleId (custom EMPLOYEE role) then login; JWT role maps to SALES_REPRESENTATIVE")
    void createWithCustomPrimaryRbac_thenLogin_ok() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        Role custom = new Role();
        custom.setName("FT Custom Role " + suffix);
        custom.setCode("ft_custom_" + suffix);
        custom.setSystemRole(false);
        custom.setStatus(RoleStatus.ACTIVE);
        custom.setLevel(RoleLevel.EMPLOYEE);
        custom = roleRepository.save(custom);
        var rbacId = custom.getId();

        String email = "staff-custom-" + suffix + "@ziyarah.com";
        String password = "Pass123!";
        String createBody = """
                {"email":"%s","password":"%s","primaryRbacRoleId":"%s","status":"ACTIVE"}
                """.formatted(email, password, rbacId);

        ResponseEntity<String> createResponse = restTemplate.postForEntity(
                baseUrl + "/users",
                new HttpEntity<>(createBody, jsonHeaders()),
                String.class
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map<?, ?> loginData = loginAndGetData(email, password);
        assertThat(loginData.get("accessToken")).isNotNull();
        assertThat(loginData.get("role")).isEqualTo("SALES_REPRESENTATIVE");
    }

    @Test
    @DisplayName("POST /users with role SUPER_ADMIN returns 400")
    void createSuperAdmin_rejected() {
        String suffix = String.valueOf(System.currentTimeMillis());
        String body = """
                {"email":"bad-sa-%s@ziyarah.com","password":"Pass123!","role":"SUPER_ADMIN","status":"ACTIVE"}
                """.formatted(suffix);
        try {
            restTemplate.postForEntity(
                    baseUrl + "/users",
                    new HttpEntity<>(body, jsonHeaders()),
                    String.class
            );
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            return;
        }
        throw new AssertionError("Expected 400");
    }

    private Map<?, ?> loginAndGetData(String email, String password) throws Exception {
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
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) parsed.get("data");
        assertThat(data).isNotNull();
        return data;
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private static HttpHeaders bearerHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        return headers;
    }
}
