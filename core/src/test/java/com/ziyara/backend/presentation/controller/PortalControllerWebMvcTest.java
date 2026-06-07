package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.BookingResponse;
import com.ziyara.backend.application.dto.response.PortalDashboardResponse;
import com.ziyara.backend.application.dto.response.ServiceResponse;
import com.ziyara.backend.application.service.HotelRoomService;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.application.service.PortalPaymentService;
import com.ziyara.backend.application.service.PortalService;
import com.ziyara.backend.application.service.ProviderMediaSubmissionService;
import com.ziyara.backend.application.service.RestaurantMenuService;
import com.ziyara.backend.application.service.ServiceImageService;
import com.ziyara.backend.application.service.ServiceProviderService;
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
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PortalController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        PortalControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class PortalControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockBean PortalService portalService;
    @MockBean PortalPaymentService portalPaymentService;
    @MockBean ProviderMediaSubmissionService providerMediaSubmissionService;
    @MockBean ServiceImageService serviceImageService;
    @MockBean RestaurantMenuService restaurantMenuService;
    @MockBean HotelRoomService hotelRoomService;
    @MockBean ServiceProviderService serviceProviderService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    private static final UUID SERVICE_ID = UUID.randomUUID();
    private static final UUID PROVIDER_ID = UUID.randomUUID();
    private static final String USER_UUID = "00000000-0000-0000-0000-000000000001";

    private void stubProviderLookup() {
        ServiceProviderResponse provider = ServiceProviderResponse.builder().id(PROVIDER_ID).build();
        when(serviceProviderService.getProviderByUserId(UUID.fromString(USER_UUID)))
                .thenReturn(java.util.Optional.of(provider));
    }

    // ── GET /portal/dashboard ─────────────────────────────────────────────────

    @Test
    void getDashboard_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/portal/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = USER_UUID, authorities = "portal:access")
    void getDashboard_withPortalAccess_returns200() throws Exception {
        stubProviderLookup();
        when(portalService.getDashboard(PROVIDER_ID)).thenReturn(PortalDashboardResponse.builder().build());

        mockMvc.perform(get("/portal/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "bookings:read")
    void getDashboard_withoutPortalAccess_returns403() throws Exception {
        mockMvc.perform(get("/portal/dashboard"))
                .andExpect(status().isForbidden());
    }

    // ── GET /portal/services ──────────────────────────────────────────────────

    @Test
    void listServices_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/portal/services"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = USER_UUID, authorities = "portal:access")
    void listServices_withPortalAccess_returns200() throws Exception {
        stubProviderLookup();
        when(portalService.getServices(eq(PROVIDER_ID), anyInt(), anyInt())).thenReturn(Page.empty());

        mockMvc.perform(get("/portal/services"))
                .andExpect(status().isOk());
    }

    // ── POST /portal/services ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_UUID, authorities = "portal:access")
    void createService_withPortalAccess_returns201() throws Exception {
        stubProviderLookup();
        ServiceResponse response = ServiceResponse.builder().id(SERVICE_ID).build();
        when(portalService.createService(eq(PROVIDER_ID), any())).thenReturn(response);

        mockMvc.perform(post("/portal/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"My Service\",\"type\":\"HOTEL\"}"))
                .andExpect(status().isCreated());
    }

    // ── GET /portal/bookings ──────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_UUID, authorities = "portal:access")
    void listBookings_withPortalAccess_returns200() throws Exception {
        stubProviderLookup();
        when(portalService.getBookings(eq(PROVIDER_ID))).thenReturn(java.util.List.of());

        mockMvc.perform(get("/portal/bookings"))
                .andExpect(status().isOk());
    }

    // ── POST /portal/bookings/{id}/payments/cash-approve ─────────────────────

    @Test
    @WithMockUser(authorities = "portal:access")
    void cashApprove_withoutPortalFinance_returns403() throws Exception {
        mockMvc.perform(post("/portal/bookings/{id}/payments/cash-approve", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = USER_UUID, authorities = {"portal:access", "portal:finance"})
    void cashApprove_withPortalFinance_returns200() throws Exception {
        stubProviderLookup();
        when(portalPaymentService.approveCashPayment(any(), any(), any())).thenReturn(null);

        mockMvc.perform(post("/portal/bookings/{id}/payments/cash-approve", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    // ── GET /portal/earnings ──────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_UUID, authorities = "portal:access")
    void getEarnings_withPortalAccess_returns200() throws Exception {
        stubProviderLookup();
        when(portalService.getEarnings(eq(PROVIDER_ID), any(), any())).thenReturn(null);

        mockMvc.perform(get("/portal/earnings"))
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
