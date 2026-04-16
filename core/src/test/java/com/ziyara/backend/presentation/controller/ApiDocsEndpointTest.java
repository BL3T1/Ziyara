package com.ziyara.backend.presentation.controller;

import com.ziyara.core.TestcontainersConfiguration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards the admin dashboard OpenAPI page: Springdoc must generate JSON without 500s.
 * Uses {@code prod} + permissive test security (same as {@link OpenApiEndpointSmokeTest}) so
 * {@code test} profile demo seeders do not run against the minimal Flyway schema.
 */
@Tag("docker")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.docker.compose.enabled=false",
                "spring.main.allow-bean-definition-overriding=true"
        })
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@Import({TestcontainersConfiguration.class, OpenApiEndpointSmokeTest.TestSecurityOverride.class})
class ApiDocsEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void springdoc_apiDocs_returnsOpenApiJson() throws Exception {
        // Servlet mapping is under server.servlet.context-path (/api/v1); use path relative to DispatcherServlet.
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").value(startsWith("3.")))
                .andExpect(jsonPath("$.info.title").value("Ziyarah API"));
    }
}
