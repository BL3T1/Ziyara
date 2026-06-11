package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.PaymentResponse;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.infrastructure.config.LocaleConfig;
import com.ziyara.backend.infrastructure.config.SecurityConfig;
import com.ziyara.backend.infrastructure.config.WebMvcConfigurationPropertiesImport;
import com.ziyara.backend.infrastructure.config.WebMvcSecuritySliceConfiguration;
import com.ziyara.backend.infrastructure.payment.PaymentGatewayProperties;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PayWebhookController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        PayWebhookControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class PayWebhookControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockBean PaymentServiceApi paymentService;
    @MockBean PaymentGatewayProperties gatewayProperties;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    // ── POST /pay/webhooks — no JWT required (public webhook endpoint) ────────

    @Test
    void webhook_emptyPayload_returns400() throws Exception {
        mockMvc.perform(post("/pay/webhooks").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void webhook_successStatus_completesPayment() throws Exception {
        when(gatewayProperties.getProvider()).thenReturn("stripe");
        PaymentResponse payResponse = new PaymentResponse();
        when(paymentService.completePaymentByGatewayReference(eq("txn-001"), eq("stripe")))
                .thenReturn(Optional.of(payResponse));

        mockMvc.perform(post("/pay/webhooks").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gateway_reference\":\"txn-001\",\"status\":\"success\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void webhook_failedStatus_failsPayment() throws Exception {
        when(gatewayProperties.getProvider()).thenReturn("stripe");

        mockMvc.perform(post("/pay/webhooks").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gateway_reference\":\"txn-002\",\"status\":\"failed\",\"error_message\":\"card_declined\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void webhook_unknownStatus_returns200WithWebhookReceived() throws Exception {
        when(gatewayProperties.getProvider()).thenReturn("stripe");

        mockMvc.perform(post("/pay/webhooks").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gateway_reference\":\"txn-003\",\"status\":\"processing\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void webhook_noGatewayRef_returns200() throws Exception {
        when(gatewayProperties.getProvider()).thenReturn("stripe");

        mockMvc.perform(post("/pay/webhooks").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event_type\":\"payment.created\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void webhook_stripeSucceededStatus_completesPayment() throws Exception {
        when(gatewayProperties.getProvider()).thenReturn("stripe");
        PaymentResponse payResponse = new PaymentResponse();
        when(paymentService.completePaymentByGatewayReference(anyString(), anyString()))
                .thenReturn(Optional.of(payResponse));

        mockMvc.perform(post("/pay/webhooks").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"pi_001\",\"status\":\"payment_intent.succeeded\"}"))
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
