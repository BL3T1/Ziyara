package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.DashboardBootstrapResponse;
import com.ziyara.backend.application.dto.response.DashboardKpiResponse;
import com.ziyara.backend.application.dto.response.DashboardLiveResponse;
import com.ziyara.backend.application.dto.response.ServiceHealthResponse;
import com.ziyara.backend.application.query.DashboardQueryHandler;
import com.ziyara.backend.application.service.DashboardBootstrapService;
import com.ziyara.backend.application.service.DashboardService;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DashboardController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        DashboardControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class DashboardControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockBean DashboardService dashboardService;
    @MockBean DashboardQueryHandler dashboardQueryHandler;
    @MockBean DashboardBootstrapService dashboardBootstrapService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    // ── GET /dashboard/kpis ───────────────────────────────────────────────────

    @Test
    void getKpis_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/dashboard/kpis"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "dashboard:read")
    void getKpis_withCompanyStaff_returns200() throws Exception {
        when(dashboardService.getKpis(any(), any())).thenReturn(DashboardKpiResponse.builder().build());

        mockMvc.perform(get("/dashboard/kpis"))
                .andExpect(status().isOk());
    }

    // ── GET /dashboard/revenue ────────────────────────────────────────────────

    @Test
    void getRevenue_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/dashboard/revenue"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "dashboard:read")
    void getRevenue_withCompanyStaff_returns200() throws Exception {
        when(dashboardService.getKpis(any(), any())).thenReturn(DashboardKpiResponse.builder().build());

        mockMvc.perform(get("/dashboard/revenue"))
                .andExpect(status().isOk());
    }

    // ── GET /dashboard/bootstrap ──────────────────────────────────────────────

    @Test
    void getBootstrap_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/dashboard/bootstrap"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "dashboard:read")
    void getBootstrap_withCompanyStaff_returns200() throws Exception {
        when(dashboardBootstrapService.load(any(), any(), anyInt()))
                .thenReturn(DashboardBootstrapResponse.builder().build());

        mockMvc.perform(get("/dashboard/bootstrap"))
                .andExpect(status().isOk());
    }

    // ── GET /dashboard/live ───────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "dashboard:read")
    void getLive_withCompanyStaff_returns200() throws Exception {
        when(dashboardBootstrapService.loadLive(any(), any(), anyInt()))
                .thenReturn(DashboardLiveResponse.builder().build());

        mockMvc.perform(get("/dashboard/live"))
                .andExpect(status().isOk());
    }

    // ── GET /dashboard/activity ───────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "dashboard:read")
    void getActivity_withCompanyStaff_returns200() throws Exception {
        when(dashboardService.getActivityFeed(anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/dashboard/activity"))
                .andExpect(status().isOk());
    }

    // ── GET /dashboard/service-health ─────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "dashboard:read")
    void getServiceHealth_withCompanyStaff_returns200() throws Exception {
        when(dashboardQueryHandler.getServiceHealth())
                .thenReturn(ServiceHealthResponse.builder()
                        .serviceCountByType(Map.of())
                        .activeBookingCountByType(Map.of())
                        .build());

        mockMvc.perform(get("/dashboard/service-health"))
                .andExpect(status().isOk());
    }

    // ── GET /dashboard/bookings ───────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "dashboard:read")
    void getBookings_withCompanyStaff_returns200() throws Exception {
        when(dashboardService.getKpis(any(), any())).thenReturn(DashboardKpiResponse.builder().build());

        mockMvc.perform(get("/dashboard/bookings"))
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
