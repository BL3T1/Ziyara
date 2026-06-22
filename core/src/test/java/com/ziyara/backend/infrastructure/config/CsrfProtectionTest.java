package com.ziyara.backend.infrastructure.config;

import com.ziyara.backend.application.dto.request.CreateExchangeRateRequest;
import com.ziyara.backend.application.dto.response.ExchangeRateResponse;
import com.ziyara.backend.application.service.CurrencyService;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.infrastructure.security.JwtCookieProperties;
import com.ziyara.backend.infrastructure.security.JwtAuthenticationFilter;
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
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ziyara.backend.presentation.controller.CurrencyController;

@WebMvcTest(controllers = CurrencyController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class
})
@ActiveProfiles("test")
class CsrfProtectionTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    CurrencyService currencyService;

    @MockBean
    JwtService jwtService;

    @MockBean
    UserDetailsService userDetailsService;

    @TestConfiguration(proxyBeanMethods = false)
    static class SecurityBeans {

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService,
                                                         SecurityContextRepository securityContextRepository,
                                                         JwtCookieProperties jwtCookieProperties,
                                                         JwtTokenBlocklistService jwtTokenBlocklistService,
                                                         JwtIdleTimeoutService jwtIdleTimeoutService) {
            return new JwtAuthenticationFilter(jwtService, userDetailsService, securityContextRepository,
                    jwtCookieProperties, jwtTokenBlocklistService, jwtIdleTimeoutService);
        }
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void postCurrencyRates_withoutCsrfToken_returns403() throws Exception {
        when(currencyService.createRate(any(CreateExchangeRateRequest.class))).thenReturn(
                ExchangeRateResponse.builder()
                        .id(java.util.UUID.randomUUID())
                        .fromCurrency("USD")
                        .toCurrency("EUR")
                        .rate(new BigDecimal("1.1"))
                        .effectiveDate(LocalDate.now())
                        .build()
        );

        mockMvc.perform(post("/currency/rates")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"fromCurrency":"USD","toCurrency":"EUR","rate":1.1}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void postCurrencyRates_withCsrfToken_returns201() throws Exception {
        when(currencyService.createRate(any(CreateExchangeRateRequest.class))).thenReturn(
                ExchangeRateResponse.builder()
                        .id(java.util.UUID.randomUUID())
                        .fromCurrency("USD")
                        .toCurrency("EUR")
                        .rate(new BigDecimal("1.1"))
                        .effectiveDate(LocalDate.now())
                        .build()
        );

        mockMvc.perform(post("/currency/rates")
                        .contentType(APPLICATION_JSON)
                        .with(csrf())
                        .content("""
                                {"fromCurrency":"USD","toCurrency":"EUR","rate":1.1}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void postCurrencyRates_withBearerHeader_ignoresCsrf_returns201() throws Exception {
        when(currencyService.createRate(any(CreateExchangeRateRequest.class))).thenReturn(
                ExchangeRateResponse.builder()
                        .id(java.util.UUID.randomUUID())
                        .fromCurrency("USD")
                        .toCurrency("EUR")
                        .rate(new BigDecimal("1.1"))
                        .effectiveDate(LocalDate.now())
                        .build()
        );

        mockMvc.perform(post("/currency/rates")
                        .header("Authorization", "Bearer test-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"fromCurrency":"USD","toCurrency":"EUR","rate":1.1}
                                """))
                .andExpect(status().isCreated());
    }
}

