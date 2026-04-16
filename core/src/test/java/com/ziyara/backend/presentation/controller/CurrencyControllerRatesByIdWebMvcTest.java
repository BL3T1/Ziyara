package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.ExchangeRateResponse;
import com.ziyara.backend.application.service.CurrencyService;
import com.ziyara.backend.infrastructure.config.LocaleConfig;
import com.ziyara.backend.infrastructure.config.SecurityConfig;
import com.ziyara.backend.infrastructure.security.JwtAuthenticationFilter;
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
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 4: GET/DELETE /currency/rates/{id}
 */
@WebMvcTest(controllers = CurrencyController.class)
@Import({SecurityConfig.class, LocaleConfig.class, CurrencyControllerRatesByIdWebMvcTest.SecurityBeans.class})
@ActiveProfiles("test")
class CurrencyControllerRatesByIdWebMvcTest {

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
        SecurityContextRepository securityContextRepository() {
            return new HttpSessionSecurityContextRepository();
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService,
                                                         SecurityContextRepository securityContextRepository) {
            return new JwtAuthenticationFilter(jwtService, userDetailsService, securityContextRepository);
        }
    }

    @Test
    @WithMockUser(roles = "ACCOUNTANT")
    void getRate_returns200() throws Exception {
        UUID id = UUID.fromString("a1000000-0000-4000-8000-000000000001");
        when(currencyService.getRate(eq(id))).thenReturn(ExchangeRateResponse.builder()
                .id(id)
                .fromCurrency("USD")
                .toCurrency("SAR")
                .rate(new BigDecimal("3.75"))
                .effectiveDate(LocalDate.of(2026, 1, 1))
                .build());

        mockMvc.perform(get("/currency/rates/{id}", id).header("Authorization", "Bearer t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fromCurrency").value("USD"))
                .andExpect(jsonPath("$.data.toCurrency").value("SAR"));
    }

    @Test
    @WithMockUser(roles = "SUPPORT_AGENT")
    void getRate_forbidden() throws Exception {
        UUID id = UUID.fromString("a2000000-0000-4000-8000-000000000001");
        mockMvc.perform(get("/currency/rates/{id}", id).header("Authorization", "Bearer t"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FINANCE_MANAGER")
    void deleteRate_returns200() throws Exception {
        UUID id = UUID.fromString("a3000000-0000-4000-8000-000000000001");
        mockMvc.perform(delete("/currency/rates/{id}", id).header("Authorization", "Bearer t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(currencyService).deleteRate(eq(id));
    }
}
