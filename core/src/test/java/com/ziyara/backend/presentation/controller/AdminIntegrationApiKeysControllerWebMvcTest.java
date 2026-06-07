package com.ziyara.backend.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziyara.backend.application.dto.request.CreateIntegrationApiKeyRequest;
import com.ziyara.backend.application.dto.response.IntegrationApiKeyCreatedResponse;
import com.ziyara.backend.application.dto.response.IntegrationApiKeySummaryResponse;
import com.ziyara.backend.application.service.IntegrationApiKeyService;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.infrastructure.config.WebMvcConfigurationPropertiesImport;
import com.ziyara.backend.infrastructure.config.LocaleConfig;
import com.ziyara.backend.infrastructure.config.WebMvcSecuritySliceConfiguration;
import com.ziyara.backend.infrastructure.config.SecurityConfig;
import com.ziyara.backend.infrastructure.security.JwtCookieProperties;
import com.ziyara.backend.infrastructure.security.JwtAuthenticationFilter;
import com.ziyara.backend.infrastructure.security.JwtIdleTimeoutService;
import com.ziyara.backend.infrastructure.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminIntegrationApiKeysController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        AdminIntegrationApiKeysControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class AdminIntegrationApiKeysControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    IntegrationApiKeyService integrationApiKeyService;

    @MockBean
    JwtService jwtService;

    @MockBean
    UserDetailsService userDetailsService;

    @TestConfiguration(proxyBeanMethods = false)
    static class SecurityBeans {
        @Bean
        SecurityContextRepository securityContextRepository() {
            return new HttpSessionSecurityContextRepository();
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService,
                                                         SecurityContextRepository securityContextRepository,
                                                         JwtCookieProperties jwtCookieProperties,
                                                         JwtTokenBlocklistService jwtTokenBlocklistService,
                                                         JwtIdleTimeoutService jwtIdleTimeoutService) {
            return new JwtAuthenticationFilter(jwtService, userDetailsService, securityContextRepository,
                    jwtCookieProperties, jwtTokenBlocklistService, jwtIdleTimeoutService);
        }
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001", roles = "SUPER_ADMIN")
    void list_ok() throws Exception {
        UUID id = UUID.fromString("d0000000-0000-4000-8000-000000000001");
        when(integrationApiKeyService.listActive()).thenReturn(List.of(
                IntegrationApiKeySummaryResponse.builder()
                        .id(id)
                        .name("Webhook")
                        .keyPrefix("ziy_deadbeef")
                        .createdAt(Instant.parse("2026-01-02T00:00:00Z"))
                        .build()
        ));

        mockMvc.perform(get("/admin/integration-api-keys").header("Authorization", "Bearer t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Webhook"));
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001", roles = "CEO")
    void list_forbiddenForCeo() throws Exception {
        mockMvc.perform(get("/admin/integration-api-keys").header("Authorization", "Bearer t"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001", roles = "SUPER_ADMIN")
    void create_returns201() throws Exception {
        UUID uid = UUID.fromString("a0000000-0000-0000-0000-000000000001");
        UUID kid = UUID.fromString("e0000000-0000-4000-8000-000000000001");
        when(integrationApiKeyService.create(any(CreateIntegrationApiKeyRequest.class), eq(uid)))
                .thenReturn(IntegrationApiKeyCreatedResponse.builder()
                        .id(kid)
                        .name("N")
                        .keyPrefix("ziy_abcd")
                        .createdAt(Instant.now())
                        .plainSecret("ziy_secret")
                        .build());

        CreateIntegrationApiKeyRequest req = CreateIntegrationApiKeyRequest.builder().name("N").build();

        mockMvc.perform(post("/admin/integration-api-keys")
                        .header("Authorization", "Bearer t")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.plainSecret").value("ziy_secret"));
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001", roles = "SUPER_ADMIN")
    void revoke_ok() throws Exception {
        UUID uid = UUID.fromString("a0000000-0000-0000-0000-000000000001");
        UUID kid = UUID.fromString("f0000000-0000-4000-8000-000000000001");

        mockMvc.perform(delete("/admin/integration-api-keys/{id}", kid).header("Authorization", "Bearer t"))
                .andExpect(status().isOk());

        verify(integrationApiKeyService).revoke(eq(kid), eq(uid));
    }
}
