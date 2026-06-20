package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.UserConsentResponse;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.application.service.UserConsentService;
import com.ziyara.backend.infrastructure.config.LocaleConfig;
import com.ziyara.backend.infrastructure.config.SecurityConfig;
import com.ziyara.backend.infrastructure.config.WebMvcConfigurationPropertiesImport;
import com.ziyara.backend.infrastructure.config.WebMvcSecuritySliceConfiguration;
import com.ziyara.backend.infrastructure.security.JwtAuthenticationFilter;
import com.ziyara.backend.infrastructure.security.JwtCookieProperties;
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
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserConsentController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        UserConsentControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class UserConsentControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockBean UserConsentService userConsentService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    // ── GET /users/me/consents ────────────────────────────────────────────────

    @Test
    void listConsents_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/users/me/consents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", authorities = "user:read")
    void listConsents_authenticated_returns200() throws Exception {
        when(userConsentService.list(any())).thenReturn(List.of());

        mockMvc.perform(get("/users/me/consents"))
                .andExpect(status().isOk());
    }

    // ── POST /users/me/consents ───────────────────────────────────────────────

    @Test
    void grantConsent_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/users/me/consents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentType\":\"MARKETING\",\"granted\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", authorities = "user:read")
    void grantConsent_authenticated_returns200() throws Exception {
        UserConsentResponse response = UserConsentResponse.builder()
                .id(UUID.randomUUID())
                .consentType("MARKETING")
                .granted(true)
                .build();
        when(userConsentService.recordGrant(any(), any(), any(), any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/users/me/consents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentType\":\"MARKETING\",\"purpose\":\"ads\",\"granted\":true}"))
                .andExpect(status().isOk());
    }

    // ── POST /users/me/consents/withdraw ──────────────────────────────────────

    @Test
    void withdrawConsent_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/users/me/consents/withdraw").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentType\":\"MARKETING\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", authorities = "user:read")
    void withdrawConsent_authenticated_returns200() throws Exception {
        mockMvc.perform(post("/users/me/consents/withdraw").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentType\":\"MARKETING\"}"))
                .andExpect(status().isOk());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SecurityBeans {

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService,
                                                         UserDetailsService userDetailsService,
                                                         SecurityContextRepository securityContextRepository,
                                                         JwtCookieProperties jwtCookieProperties,
                                                         JwtTokenBlocklistService jwtTokenBlocklistService,
                                                         JwtIdleTimeoutService jwtIdleTimeoutService) {
            return new JwtAuthenticationFilter(jwtService, userDetailsService, securityContextRepository,
                    jwtCookieProperties, jwtTokenBlocklistService, jwtIdleTimeoutService);
        }
    }
}
