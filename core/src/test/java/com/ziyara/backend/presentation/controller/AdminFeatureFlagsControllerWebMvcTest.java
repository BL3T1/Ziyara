package com.ziyara.backend.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziyara.backend.application.dto.request.UpsertFeatureFlagRequest;
import com.ziyara.backend.application.dto.response.FeatureFlagResponse;
import com.ziyara.backend.application.service.FeatureFlagService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminFeatureFlagsController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        AdminFeatureFlagsControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class AdminFeatureFlagsControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    FeatureFlagService featureFlagService;

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
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001", roles = "CEO")
    void list_okForCeo() throws Exception {
        UUID id = UUID.fromString("b0000000-0000-4000-8000-000000000001");
        when(featureFlagService.listAll()).thenReturn(List.of(
                FeatureFlagResponse.builder()
                        .id(id)
                        .flagKey("test.flag")
                        .enabled(true)
                        .description("d")
                        .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                        .build()
        ));

        mockMvc.perform(get("/admin/feature-flags").header("Authorization", "Bearer t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].flagKey").value("test.flag"))
                .andExpect(jsonPath("$.data[0].enabled").value(true));
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001", roles = "SUPPORT_AGENT")
    void list_forbidden() throws Exception {
        mockMvc.perform(get("/admin/feature-flags").header("Authorization", "Bearer t"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001", roles = "GENERAL_MANAGER")
    void put_ok() throws Exception {
        UUID uid = UUID.fromString("a0000000-0000-0000-0000-000000000001");
        FeatureFlagResponse body = FeatureFlagResponse.builder()
                .id(UUID.fromString("c0000000-0000-4000-8000-000000000001"))
                .flagKey("x.y")
                .enabled(false)
                .build();
        when(featureFlagService.upsert(any(UpsertFeatureFlagRequest.class), eq(uid))).thenReturn(body);

        UpsertFeatureFlagRequest req = UpsertFeatureFlagRequest.builder()
                .flagKey("x.y")
                .enabled(false)
                .build();

        mockMvc.perform(put("/admin/feature-flags")
                        .header("Authorization", "Bearer t")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.flagKey").value("x.y"));
    }
}
