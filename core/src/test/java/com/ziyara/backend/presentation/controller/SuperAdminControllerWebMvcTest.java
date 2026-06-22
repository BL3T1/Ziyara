package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.application.service.SuperAdminRecoveryService;
import com.ziyara.backend.infrastructure.config.LocaleConfig;
import com.ziyara.backend.infrastructure.config.SecurityConfig;
import com.ziyara.backend.infrastructure.config.WebMvcConfigurationPropertiesImport;
import com.ziyara.backend.infrastructure.config.WebMvcSecuritySliceConfiguration;
import com.ziyara.backend.infrastructure.security.JwtAuthenticationFilter;
import com.ziyara.backend.infrastructure.security.JwtCookieProperties;
import com.ziyara.backend.infrastructure.security.JwtIdleTimeoutService;
import com.ziyara.backend.infrastructure.security.JwtService;
import com.ziyara.backend.modules.booking.api.BookingServiceApi;
import com.ziyara.backend.modules.payment.api.PaymentServiceApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SuperAdminController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        SuperAdminControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class SuperAdminControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockBean SuperAdminRecoveryService superAdminRecoveryService;
    @MockBean BookingServiceApi bookingService;
    @MockBean PaymentServiceApi paymentService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    // ── GET /admin/super/customers/search ─────────────────────────────────────

    @Test
    void searchCustomers_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/admin/super/customers/search"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "customers:read")
    void searchCustomers_withPermission_returns200() throws Exception {
        when(superAdminRecoveryService.searchCustomers(any(), anyInt()))
                .thenReturn(java.util.List.of());

        mockMvc.perform(get("/admin/super/customers/search"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "bookings:read")
    void searchCustomers_withoutCustomersRead_returns403() throws Exception {
        mockMvc.perform(get("/admin/super/customers/search"))
                .andExpect(status().isForbidden());
    }

    // ── GET /admin/super/deleted ───────────────────────────────────────────────

    @Test
    void listDeleted_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/admin/super/deleted"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "deleted_items:read")
    void listDeleted_withPermission_returns200() throws Exception {
        when(superAdminRecoveryService.listRecentDeleted(anyInt(), any()))
                .thenReturn(java.util.List.of());

        mockMvc.perform(get("/admin/super/deleted"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "bookings:read")
    void listDeleted_withoutDeletedItemsRead_returns403() throws Exception {
        mockMvc.perform(get("/admin/super/deleted"))
                .andExpect(status().isForbidden());
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
