package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.PriceBreakdownResponse;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.infrastructure.config.LocaleConfig;
import com.ziyara.backend.infrastructure.config.SecurityConfig;
import com.ziyara.backend.infrastructure.config.WebMvcConfigurationPropertiesImport;
import com.ziyara.backend.infrastructure.config.WebMvcSecuritySliceConfiguration;
import com.ziyara.backend.infrastructure.security.JwtAuthenticationFilter;
import com.ziyara.backend.infrastructure.security.JwtCookieProperties;
import com.ziyara.backend.infrastructure.security.JwtIdleTimeoutService;
import com.ziyara.backend.infrastructure.security.JwtService;
import com.ziyara.backend.modules.pricing.api.PricingEngineApi;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PricingController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        PricingControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class PricingControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockBean PricingEngineApi pricingService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    static final String PREVIEW_BODY = "{\"serviceId\":\"00000000-0000-0000-0000-000000000001\",\"checkInDate\":\"2026-12-01\"}";

    // ── POST /pricing/preview ─────────────────────────────────────────────────

    @Test
    void preview_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/pricing/preview").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PREVIEW_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "user:read")
    void preview_authenticated_returns200() throws Exception {
        PriceBreakdownResponse response = PriceBreakdownResponse.builder()
                .baseAmount(java.math.BigDecimal.valueOf(100))
                .build();
        when(pricingService.calculatePrice(any())).thenReturn(response);

        mockMvc.perform(post("/pricing/preview").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PREVIEW_BODY))
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
