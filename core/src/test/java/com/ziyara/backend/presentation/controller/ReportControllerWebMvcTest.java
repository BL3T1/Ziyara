package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.BookingReportResponse;
import com.ziyara.backend.application.dto.response.RevenueReportResponse;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.application.service.ReportExportService;
import com.ziyara.backend.application.service.ReportService;
import com.ziyara.backend.application.service.SuperAdminRecoveryService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReportController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        ReportControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class ReportControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockBean ReportService reportService;
    @MockBean ReportExportService reportExportService;
    @MockBean SuperAdminRecoveryService superAdminRecoveryService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    // ── GET /reports/revenue ──────────────────────────────────────────────────

    @Test
    void getRevenue_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/reports/revenue")
                        .param("start", "2026-01-01")
                        .param("end", "2026-01-31"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "reports:read")
    void getRevenue_withReportsRead_returns200() throws Exception {
        RevenueReportResponse response = RevenueReportResponse.builder()
                .totalRevenue(BigDecimal.valueOf(1000))
                .build();
        when(reportService.generateRevenueReport(any(), any(), anyString(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/reports/revenue")
                        .param("start", "2026-01-01")
                        .param("end", "2026-01-31"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "bookings:read")
    void getRevenue_withoutReportsRead_returns403() throws Exception {
        mockMvc.perform(get("/reports/revenue")
                        .param("start", "2026-01-01")
                        .param("end", "2026-01-31"))
                .andExpect(status().isForbidden());
    }

    // ── GET /reports/bookings ─────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "reports:read")
    void getBookingReport_withReportsRead_returns200() throws Exception {
        BookingReportResponse response = BookingReportResponse.builder()
                .totalBookings(10)
                .build();
        when(reportService.generateBookingReport(any(), any(), anyString(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/reports/bookings")
                        .param("start", "2026-01-01")
                        .param("end", "2026-01-31"))
                .andExpect(status().isOk());
    }

    // ── GET /reports/analytics ────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "reports:read")
    void getAnalytics_withReportsRead_returns200() throws Exception {
        when(reportService.getAnalytics(any(), any())).thenReturn(Map.of("topProviders", List.of()));

        mockMvc.perform(get("/reports/analytics")
                        .param("start", "2026-01-01")
                        .param("end", "2026-01-31"))
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
