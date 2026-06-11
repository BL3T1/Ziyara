package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.AdminPayoutResponse;
import com.ziyara.backend.application.dto.response.AdminPayoutSummaryResponse;
import com.ziyara.backend.application.service.AdminPayoutService;
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
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminPayoutController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        AdminPayoutControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class AdminPayoutControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockBean AdminPayoutService adminPayoutService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    private static final UUID PAYOUT_ID = UUID.randomUUID();

    // ── GET /admin/payouts/summary ────────────────────────────────────────────

    @Test
    void getSummary_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/admin/payouts/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "payouts:read")
    void getSummary_withPayoutsRead_returns200() throws Exception {
        when(adminPayoutService.getSummary(any(), any()))
                .thenReturn(AdminPayoutSummaryResponse.builder().build());

        mockMvc.perform(get("/admin/payouts/summary"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "bookings:read")
    void getSummary_withoutPayoutsRead_returns403() throws Exception {
        mockMvc.perform(get("/admin/payouts/summary"))
                .andExpect(status().isForbidden());
    }

    // ── GET /admin/payouts ────────────────────────────────────────────────────

    @Test
    void listPayouts_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/admin/payouts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "payouts:read")
    void listPayouts_withPayoutsRead_returns200() throws Exception {
        when(adminPayoutService.listPayouts(anyInt(), anyInt(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/admin/payouts"))
                .andExpect(status().isOk());
    }

    // ── GET /admin/payouts/{id} ───────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "payouts:read")
    void getById_withPayoutsRead_returns200() throws Exception {
        when(adminPayoutService.getById(PAYOUT_ID))
                .thenReturn(AdminPayoutResponse.builder().id(PAYOUT_ID).build());

        mockMvc.perform(get("/admin/payouts/{id}", PAYOUT_ID))
                .andExpect(status().isOk());
    }

    // ── POST /admin/payouts/{id}/approve ──────────────────────────────────────

    @Test
    void approvePayout_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/admin/payouts/{id}/approve", PAYOUT_ID).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "payouts:write")
    void approvePayout_withPayoutsWrite_returns200() throws Exception {
        when(adminPayoutService.approve(eq(PAYOUT_ID), any()))
                .thenReturn(AdminPayoutResponse.builder().id(PAYOUT_ID).build());

        mockMvc.perform(post("/admin/payouts/{id}/approve", PAYOUT_ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "payouts:read")
    void approvePayout_withoutPayoutsWrite_returns403() throws Exception {
        mockMvc.perform(post("/admin/payouts/{id}/approve", PAYOUT_ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ── POST /admin/payouts/{id}/mark-paid ────────────────────────────────────

    @Test
    @WithMockUser(authorities = "payouts:approve")
    void markPaid_withPayoutsApprove_returns200() throws Exception {
        when(adminPayoutService.markPaid(eq(PAYOUT_ID), any()))
                .thenReturn(AdminPayoutResponse.builder().id(PAYOUT_ID).build());

        mockMvc.perform(post("/admin/payouts/{id}/mark-paid", PAYOUT_ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "payouts:write")
    void markPaid_withoutPayoutsApprove_returns403() throws Exception {
        mockMvc.perform(post("/admin/payouts/{id}/mark-paid", PAYOUT_ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ── POST /admin/payouts/{id}/hold ─────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "payouts:write")
    void holdPayout_withPayoutsWrite_returns200() throws Exception {
        when(adminPayoutService.hold(PAYOUT_ID))
                .thenReturn(AdminPayoutResponse.builder().id(PAYOUT_ID).build());

        mockMvc.perform(post("/admin/payouts/{id}/hold", PAYOUT_ID).with(csrf()))
                .andExpect(status().isOk());
    }

    // ── POST /admin/payouts/{id}/cancel ───────────────────────────────────────

    @Test
    @WithMockUser(authorities = "payouts:write")
    void cancelPayout_withPayoutsWrite_returns200() throws Exception {
        when(adminPayoutService.cancel(PAYOUT_ID))
                .thenReturn(AdminPayoutResponse.builder().id(PAYOUT_ID).build());

        mockMvc.perform(post("/admin/payouts/{id}/cancel", PAYOUT_ID).with(csrf()))
                .andExpect(status().isOk());
    }

    // ── POST /admin/payouts/bulk/approve ──────────────────────────────────────

    @Test
    @WithMockUser(authorities = "payouts:approve")
    void bulkApprove_withPayoutsApprove_returns200() throws Exception {
        when(adminPayoutService.bulkApprove(any())).thenReturn(Map.of("processed", 1));

        mockMvc.perform(post("/admin/payouts/bulk/approve").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"" + PAYOUT_ID + "\"]}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "payouts:write")
    void bulkApprove_withoutPayoutsApprove_returns403() throws Exception {
        mockMvc.perform(post("/admin/payouts/bulk/approve").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"" + PAYOUT_ID + "\"]}"))
                .andExpect(status().isForbidden());
    }

    // ── GET /admin/payouts/export ──────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "payouts:read")
    void exportCsv_withPayoutsRead_returns200() throws Exception {
        when(adminPayoutService.exportCsv(any(), any(), any())).thenReturn("id,amount\n".getBytes());

        mockMvc.perform(get("/admin/payouts/export"))
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
