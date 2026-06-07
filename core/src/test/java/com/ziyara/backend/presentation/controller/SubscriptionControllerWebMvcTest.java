package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.CustomerSubscriptionResponse;
import com.ziyara.backend.application.dto.response.PlanResponse;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.application.service.SubscriptionService;
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

@WebMvcTest(controllers = SubscriptionController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        SubscriptionControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class SubscriptionControllerWebMvcTest {

    static final UUID PROVIDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    static final UUID ADD_ON_ID = UUID.fromString("00000000-0000-0000-0000-000000000088");
    static final String BASE = "/providers/" + PROVIDER_ID + "/subscription";

    @Autowired MockMvc mockMvc;

    @MockBean SubscriptionService subscriptionService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    // ── GET /providers/{id}/subscription/plans ────────────────────────────────

    @Test
    void listPlans_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE + "/plans"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "billing:read")
    void listPlans_withCompanyStaff_returns200() throws Exception {
        when(subscriptionService.listPlans()).thenReturn(List.of());

        mockMvc.perform(get(BASE + "/plans"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "portal:access")
    void listPlans_withPortalAccess_returns200() throws Exception {
        when(subscriptionService.listPlans()).thenReturn(List.of());

        mockMvc.perform(get(BASE + "/plans"))
                .andExpect(status().isOk());
    }

    // ── GET /providers/{id}/subscription ─────────────────────────────────────

    @Test
    void getSubscription_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(BASE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "billing:read")
    void getSubscription_withCompanyStaff_returns200() throws Exception {
        when(subscriptionService.getSubscription(PROVIDER_ID))
                .thenReturn(CustomerSubscriptionResponse.builder().providerId(PROVIDER_ID).build());

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk());
    }

    // ── POST /providers/{id}/subscription/activate ────────────────────────────

    @Test
    void activate_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post(BASE + "/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planCode\":\"BASIC\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "billing:read")
    void activate_withoutDiscountApprove_returns403() throws Exception {
        mockMvc.perform(post(BASE + "/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planCode\":\"BASIC\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "discounts:approve")
    void activate_withDiscountApprove_returns200() throws Exception {
        when(subscriptionService.activateSubscription(any(), any()))
                .thenReturn(CustomerSubscriptionResponse.builder().providerId(PROVIDER_ID).build());

        mockMvc.perform(post(BASE + "/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planCode\":\"BASIC\"}"))
                .andExpect(status().isOk());
    }

    // ── POST /providers/{id}/subscription/add-ons ─────────────────────────────

    @Test
    void addSeatExpansion_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post(BASE + "/add-ons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addOnCode\":\"SEATS_5\",\"displayName\":\"5 Extra Seats\",\"extraSeats\":5,\"price\":99.00}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "discounts:approve")
    void addSeatExpansion_withDiscountApprove_returns200() throws Exception {
        when(subscriptionService.addSeatExpansion(any(), any()))
                .thenReturn(CustomerSubscriptionResponse.builder().providerId(PROVIDER_ID).build());

        mockMvc.perform(post(BASE + "/add-ons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addOnCode\":\"SEATS_5\",\"displayName\":\"5 Extra Seats\",\"extraSeats\":5,\"price\":99.00}"))
                .andExpect(status().isOk());
    }

    // ── DELETE /providers/{id}/subscription/add-ons/{addOnId} ────────────────

    @Test
    void cancelAddOn_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete(BASE + "/add-ons/" + ADD_ON_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "discounts:approve")
    void cancelAddOn_withDiscountApprove_returns200() throws Exception {
        when(subscriptionService.cancelAddOn(any(), any()))
                .thenReturn(CustomerSubscriptionResponse.builder().providerId(PROVIDER_ID).build());

        mockMvc.perform(delete(BASE + "/add-ons/" + ADD_ON_ID))
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
