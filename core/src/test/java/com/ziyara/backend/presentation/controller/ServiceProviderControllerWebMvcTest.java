package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.ServiceProviderResponse;
import com.ziyara.backend.application.service.AuthService;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.application.service.PortalService;
import com.ziyara.backend.application.service.ServiceProviderService;
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
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ServiceProviderController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        ServiceProviderControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class ServiceProviderControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockBean ServiceProviderService providerService;
    @MockBean PortalService portalService;
    @MockBean AuthService authService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    private static final UUID PROVIDER_ID = UUID.randomUUID();

    // ── GET /providers ────────────────────────────────────────────────────────

    @Test
    void listProviders_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/providers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "providers:read")
    void listProviders_withCompanyStaff_returns200() throws Exception {
        when(providerService.getProvidersPage(anyInt(), anyInt(), any(), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/providers"))
                .andExpect(status().isOk());
    }

    // ── GET /providers/{id} ───────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "providers:read")
    void getProvider_returns200() throws Exception {
        ServiceProviderResponse response = ServiceProviderResponse.builder().id(PROVIDER_ID).build();
        when(providerService.getProvider(PROVIDER_ID)).thenReturn(response);

        mockMvc.perform(get("/providers/{id}", PROVIDER_ID))
                .andExpect(status().isOk());
    }

    // ── POST /providers ───────────────────────────────────────────────────────

    @Test
    void createProvider_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/providers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Provider\",\"type\":\"RESTAURANT\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", authorities = "providers:write")
    void createProvider_withProviderWrite_returns201() throws Exception {
        ServiceProviderResponse response = ServiceProviderResponse.builder().id(PROVIDER_ID).build();
        when(providerService.createProvider(any(), any())).thenReturn(response);

        mockMvc.perform(post("/providers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Provider\",\"type\":\"RESTAURANT\",\"phone\":\"+971500000001\",\"address\":\"123 Main St\",\"globalRate\":0.10}"))
                .andExpect(status().isCreated());
    }

    // ── POST /providers/{id}/approve ──────────────────────────────────────────

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", authorities = "providers:approve")
    void approveProvider_withPermission_returns200() throws Exception {
        ServiceProviderResponse response = ServiceProviderResponse.builder().id(PROVIDER_ID).build();
        when(providerService.approveProvider(eq(PROVIDER_ID), any())).thenReturn(response);

        mockMvc.perform(post("/providers/{id}/approve", PROVIDER_ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "providers:read")
    void approveProvider_withoutPermission_returns403() throws Exception {
        mockMvc.perform(post("/providers/{id}/approve", PROVIDER_ID).with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ── PATCH /providers/{id} ─────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "providers:write")
    void updateProvider_withCompanyStaff_returns200() throws Exception {
        ServiceProviderResponse response = ServiceProviderResponse.builder().id(PROVIDER_ID).build();
        when(providerService.updateProvider(eq(PROVIDER_ID), any())).thenReturn(response);

        mockMvc.perform(patch("/providers/{id}", PROVIDER_ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Name\"}"))
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
