package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.ProviderSubscriptionResponse;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.application.service.ProviderSubscriptionService;
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
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProviderSubscriptionController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        ProviderSubscriptionControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class ProviderSubscriptionControllerWebMvcTest {

    static final UUID PROVIDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Autowired MockMvc mockMvc;

    @MockBean ProviderSubscriptionService subscriptionService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    // ── GET /admin/subscriptions ──────────────────────────────────────────────

    @Test
    void listAll_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/admin/subscriptions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "bookings:read")
    void listAll_withoutSubscriptionsRead_returns403() throws Exception {
        mockMvc.perform(get("/admin/subscriptions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "providers:read")
    void listAll_withSubscriptionsRead_returns200() throws Exception {
        when(subscriptionService.listAll()).thenReturn(List.of());

        mockMvc.perform(get("/admin/subscriptions"))
                .andExpect(status().isOk());
    }

    // ── GET /admin/subscriptions/{providerId} ─────────────────────────────────

    @Test
    void get_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/admin/subscriptions/" + PROVIDER_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "providers:read")
    void get_withSubscriptionsRead_returns200() throws Exception {
        when(subscriptionService.getByProviderId(PROVIDER_ID))
                .thenReturn(ProviderSubscriptionResponse.builder().providerId(PROVIDER_ID).build());

        mockMvc.perform(get("/admin/subscriptions/" + PROVIDER_ID))
                .andExpect(status().isOk());
    }

    // ── PUT /admin/subscriptions/{providerId} ─────────────────────────────────

    @Test
    void upsert_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/admin/subscriptions/" + PROVIDER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\":\"BASIC\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "bookings:read")
    void upsert_withoutPermission_returns403() throws Exception {
        mockMvc.perform(put("/admin/subscriptions/" + PROVIDER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\":\"BASIC\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "providers:read")
    void upsert_withSubscriptionsRead_returns200() throws Exception {
        when(subscriptionService.upsert(any(), any()))
                .thenReturn(ProviderSubscriptionResponse.builder().providerId(PROVIDER_ID).build());

        mockMvc.perform(put("/admin/subscriptions/" + PROVIDER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\":\"BASIC\"}"))
                .andExpect(status().isOk());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SecurityBeans {
        @Bean
        SecurityContextRepository securityContextRepository() {
            return new HttpSessionSecurityContextRepository();
        }

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
