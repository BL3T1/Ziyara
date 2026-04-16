package com.ziyara.backend.presentation.controller;

import com.ziyara.core.TestcontainersConfiguration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * OpenAPI / handler wiring smoke tests. {@link TestSecurityOverride} disables CSRF and opens auth
 * for the test context only so MockMvc can hit endpoints; do not copy to production.
 */
@Tag("docker")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.docker.compose.enabled=false",
                "spring.main.allow-bean-definition-overriding=true"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@Import({TestcontainersConfiguration.class, OpenApiEndpointSmokeTest.TestSecurityOverride.class})
class OpenApiEndpointSmokeTest {

    @Autowired MockMvc mockMvc;
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping handlerMapping;

    @Test
    void openApi_AllPaths_areMappedAndNotServerError() throws Exception {
        assertThat(handlerMapping.getHandlerMethods()).isNotEmpty();

        for (var entry : handlerMapping.getHandlerMethods().entrySet()) {
            RequestMappingInfo info = entry.getKey();

            Set<String> patterns = getPatternValues(info);
            if (patterns.isEmpty()) {
                continue;
            }

            Set<RequestMethod> requestMethods = info.getMethodsCondition().getMethods();
            Collection<HttpMethod> httpMethods = requestMethods.isEmpty()
                    ? java.util.List.of(HttpMethod.GET)
                    : requestMethods.stream().map(m -> HttpMethod.valueOf(m.name())).toList();

            for (String rawPath : patterns) {
                String resolvedPath = replacePathParams(rawPath);
                if (!resolvedPath.startsWith("/api/v1")) {
                    resolvedPath = "/api/v1" + (resolvedPath.startsWith("/") ? "" : "/") + resolvedPath;
                }

                for (HttpMethod method : httpMethods) {
                    MockHttpServletRequestBuilder req = request(method, resolvedPath);
                    if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH) {
                        req = req.contentType(MediaType.APPLICATION_JSON).content("{}");
                    }

                    int statusCode = mockMvc.perform(req).andReturn().getResponse().getStatus();

                    assertThat(statusCode)
                            .withFailMessage("Endpoint %s %s returned %s", method, resolvedPath, statusCode)
                            .isNotEqualTo(404);
                }
            }
        }
    }

    @Test
    void actuatorHealth_isOk() throws Exception {
        int statusCode = mockMvc.perform(request(HttpMethod.GET, "/actuator/health"))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(statusCode).isEqualTo(200);
    }

    private static Set<String> getPatternValues(RequestMappingInfo info) {
        var pathPatterns = info.getPathPatternsCondition();
        if (pathPatterns != null) {
            return pathPatterns.getPatternValues();
        }
        var patterns = info.getPatternsCondition();
        if (patterns != null) {
            return patterns.getPatterns();
        }
        return Set.of();
    }

    private static String replacePathParams(String rawPath) {
        // Replace templated segments like /users/{id} with something harmless.
        // Prefer UUID-like values when the param name hints it.
        String out = rawPath;
        while (true) {
            int start = out.indexOf('{');
            int end = out.indexOf('}');
            if (start < 0 || end < 0 || end < start) {
                return out;
            }
            String paramName = out.substring(start + 1, end).toLowerCase(Locale.ROOT);
            String replacement = (paramName.contains("uuid") || paramName.endsWith("id"))
                    ? "00000000-0000-0000-0000-000000000001"
                    : "1";
            out = out.substring(0, start) + replacement + out.substring(end + 1);
        }
    }

    /**
     * Test-only security: CSRF disabled so smoke requests succeed; not used in production.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class TestSecurityOverride {
        @org.springframework.context.annotation.Bean(name = "authFilterChain")
        @Order(1)
        SecurityFilterChain authFilterChain(HttpSecurity http) throws Exception {
            http.securityMatcher(
                            "/auth/**",
                            "/actuator/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/api-docs/**",
                            "/v3/api-docs/**")
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        @org.springframework.context.annotation.Bean(name = "securityFilterChain")
        @Order(2)
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.securityMatcher("/**")
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }
}

