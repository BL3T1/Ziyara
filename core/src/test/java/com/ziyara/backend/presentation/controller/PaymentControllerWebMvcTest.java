package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.PaymentResponse;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.infrastructure.config.LocaleConfig;
import com.ziyara.backend.infrastructure.config.SecurityConfig;
import com.ziyara.backend.infrastructure.config.WebMvcConfigurationPropertiesImport;
import com.ziyara.backend.infrastructure.config.WebMvcSecuritySliceConfiguration;
import com.ziyara.backend.infrastructure.security.JwtAuthenticationFilter;
import com.ziyara.backend.infrastructure.security.JwtCookieProperties;
import com.ziyara.backend.infrastructure.security.JwtIdleTimeoutService;
import com.ziyara.backend.infrastructure.security.JwtService;
import com.ziyara.backend.modules.payment.api.PaymentServiceApi;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        PaymentControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class PaymentControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockBean PaymentServiceApi paymentService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    // ── Unauthenticated → 401 ──────────────────────────────────────────────

    @Test
    void processPayment_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/payments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":\"00000000-0000-0000-0000-000000000001\",\"amount\":100,\"currency\":\"USD\",\"method\":\"CARD\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listPayments_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/payments")).andExpect(status().isUnauthorized());
    }

    @Test
    void getPayment_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/payments/{id}", UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test
    void completePayment_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/payments/{id}/complete", UUID.randomUUID()).with(csrf())
                        .param("reference", "ref")
                        .param("gateway", "stripe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void failPayment_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/payments/{id}/fail", UUID.randomUUID()).with(csrf())
                        .param("reason", "card_declined"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refund_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/payments/{id}/refund", UUID.randomUUID()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50,\"reason\":\"customer request\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ── Customer role → 403 on privileged endpoints ────────────────────────

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void listPayments_customerRole_returns403() throws Exception {
        mockMvc.perform(get("/payments")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getPayment_customerRole_returns403() throws Exception {
        mockMvc.perform(get("/payments/{id}", UUID.randomUUID())).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void completePayment_customerRole_returns403() throws Exception {
        mockMvc.perform(post("/payments/{id}/complete", UUID.randomUUID()).with(csrf())
                        .param("reference", "ref")
                        .param("gateway", "stripe"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void failPayment_customerRole_returns403() throws Exception {
        mockMvc.perform(post("/payments/{id}/fail", UUID.randomUUID()).with(csrf())
                        .param("reason", "card_declined"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void refund_customerRole_returns403() throws Exception {
        mockMvc.perform(post("/payments/{id}/refund", UUID.randomUUID()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50,\"reason\":\"customer request\"}"))
                .andExpect(status().isForbidden());
    }

    // ── Finance staff → 200/201 on read endpoints ──────────────────────────

    @Test
    @WithMockUser(authorities = "payments:read")
    void listPayments_financeManager_returns200() throws Exception {
        when(paymentService.getPayments(0, 20, null)).thenReturn(Page.empty());
        mockMvc.perform(get("/payments")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "payments:read")
    void getPayment_financeManager_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(paymentService.getPayment(id)).thenReturn(new PaymentResponse());
        mockMvc.perform(get("/payments/{id}", id)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "payments:read")
    void listPayments_accountant_returns200() throws Exception {
        when(paymentService.getPayments(0, 20, null)).thenReturn(Page.empty());
        mockMvc.perform(get("/payments")).andExpect(status().isOk());
    }

    // ── SUPER_ADMIN → 200 on complete/fail ────────────────────────────────

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001", authorities = "payments:write")
    void completePayment_superAdmin_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(paymentService.completePayment(eq(id), eq("txn-ref"), eq("stripe")))
                .thenReturn(new PaymentResponse());
        mockMvc.perform(post("/payments/{id}/complete", id).with(csrf())
                        .param("reference", "txn-ref")
                        .param("gateway", "stripe"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001", authorities = "payments:write")
    void failPayment_superAdmin_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(paymentService.failPayment(eq(id), eq("card_declined")))
                .thenReturn(new PaymentResponse());
        mockMvc.perform(post("/payments/{id}/fail", id).with(csrf())
                        .param("reason", "card_declined"))
                .andExpect(status().isOk());
    }

    // ── Refund — FINANCE_MANAGER only ─────────────────────────────────────

    @Test
    @WithMockUser(authorities = "other:permission")
    void refund_accountantRole_returns403() throws Exception {
        mockMvc.perform(post("/payments/{id}/refund", UUID.randomUUID()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50,\"reason\":\"customer request\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000002", authorities = "payments:write")
    void refund_financeManager_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(paymentService.refund(eq(id), any(), any())).thenReturn(new com.ziyara.backend.application.dto.response.RefundResponse());
        mockMvc.perform(post("/payments/{id}/refund", id).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50,\"reason\":\"customer request\"}"))
                .andExpect(status().isCreated());
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
