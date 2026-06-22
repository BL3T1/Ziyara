package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.DiscountResponse;
import com.ziyara.backend.application.query.DiscountQueryHandler;
import com.ziyara.backend.application.service.DiscountCodeService;
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

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DiscountController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        DiscountControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class DiscountControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockBean DiscountCodeService discountService;
    @MockBean DiscountQueryHandler discountQueryHandler;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    private static final UUID DISCOUNT_ID = UUID.randomUUID();

    // ── GET /discounts ────────────────────────────────────────────────────────

    @Test
    void listDiscounts_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/discounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "discounts:read")
    void listDiscounts_withCompanyStaff_returns200() throws Exception {
        when(discountQueryHandler.findPage(anyInt(), anyInt(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/discounts"))
                .andExpect(status().isOk());
    }

    // ── GET /discounts/{id} ────────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "discounts:read")
    void getById_found_returns200() throws Exception {
        DiscountResponse response = DiscountResponse.builder().id(DISCOUNT_ID).code("SAVE10").build();
        when(discountQueryHandler.findById(DISCOUNT_ID)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/discounts/{id}", DISCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("SAVE10"));
    }

    @Test
    @WithMockUser(authorities = "discounts:read")
    void getById_notFound_returns404() throws Exception {
        when(discountQueryHandler.findById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/discounts/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ── POST /discounts ────────────────────────────────────────────────────────

    @Test
    void createDiscount_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/discounts").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SAVE20\",\"type\":\"PERCENTAGE\",\"value\":20}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "discounts:write")
    void createDiscount_withPermission_returns201() throws Exception {
        DiscountResponse response = DiscountResponse.builder().id(DISCOUNT_ID).code("SAVE20").build();
        when(discountService.create(any())).thenReturn(response);

        mockMvc.perform(post("/discounts").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SAVE20\",\"type\":\"PERCENTAGE\",\"value\":20,\"sponsor\":\"COMPANY\",\"endDate\":\"2027-12-31T23:59:59\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("SAVE20"));
    }

    @Test
    @WithMockUser(authorities = "bookings:read")
    void createDiscount_withoutDiscountsWrite_returns403() throws Exception {
        mockMvc.perform(post("/discounts").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SAVE20\",\"type\":\"PERCENTAGE\",\"value\":20,\"endDate\":\"2027-12-31T23:59:59\"}"))
                .andExpect(status().isForbidden());
    }

    // ── POST /discounts/{id}/approve ───────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "discounts:approve")
    void approve_withPermission_returns200() throws Exception {
        DiscountResponse response = DiscountResponse.builder().id(DISCOUNT_ID).code("SAVE10").build();
        when(discountService.approve(DISCOUNT_ID)).thenReturn(response);

        mockMvc.perform(post("/discounts/{id}/approve", DISCOUNT_ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "discounts:write")
    void approve_withoutApprovePermission_returns403() throws Exception {
        mockMvc.perform(post("/discounts/{id}/approve", DISCOUNT_ID).with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /discounts/{id} ────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "discounts:read")
    void deleteDiscount_withCompanyStaff_returns200() throws Exception {
        mockMvc.perform(delete("/discounts/{id}", DISCOUNT_ID).with(csrf()))
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
