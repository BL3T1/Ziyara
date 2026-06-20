package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.ProviderMapPinResponse;
import com.ziyara.backend.application.dto.response.ServiceProviderResponse;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.application.service.MapService;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MapController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        MapControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class MapControllerWebMvcTest {

    static final String USER_UUID = "00000000-0000-0000-0000-000000000001";
    static final UUID PROVIDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    static final UUID BOOKING_ID = UUID.fromString("00000000-0000-0000-0000-000000000050");

    @Autowired MockMvc mockMvc;

    @MockBean MapService mapService;
    @MockBean ServiceProviderService serviceProviderService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    // ── GET /map/providers ────────────────────────────────────────────────────

    @Test
    void getProviderPins_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/map/providers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "bookings:read")
    void getProviderPins_withoutSubscriptionsRead_returns403() throws Exception {
        mockMvc.perform(get("/map/providers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "providers:read")
    void getProviderPins_withSubscriptionsRead_returns200() throws Exception {
        when(mapService.getProviderPins(any())).thenReturn(List.of());

        mockMvc.perform(get("/map/providers"))
                .andExpect(status().isOk());
    }

    // ── GET /map/portal/pins ──────────────────────────────────────────────────

    @Test
    void getPortalPins_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/map/portal/pins"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "providers:read")
    void getPortalPins_withoutPortalAccess_returns403() throws Exception {
        mockMvc.perform(get("/map/portal/pins"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = USER_UUID, authorities = "portal:access")
    void getPortalPins_withPortalAccess_returns200() throws Exception {
        ServiceProviderResponse provider = ServiceProviderResponse.builder()
                .id(PROVIDER_ID)
                .build();
        when(serviceProviderService.getProviderByUserId(eq(UUID.fromString(USER_UUID))))
                .thenReturn(Optional.of(provider));
        when(mapService.getPortalPins(PROVIDER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/map/portal/pins"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USER_UUID, authorities = "portal:access")
    void getPortalPins_noProviderProfile_returns403() throws Exception {
        when(serviceProviderService.getProviderByUserId(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/map/portal/pins"))
                .andExpect(status().isForbidden());
    }

    // ── GET /map/delivery/{bookingId} ─────────────────────────────────────────

    @Test
    void getDeliveryLocation_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/map/delivery/" + BOOKING_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "user:read")
    void getDeliveryLocation_authenticated_returns404() throws Exception {
        mockMvc.perform(get("/map/delivery/" + BOOKING_ID))
                .andExpect(status().isNotFound());
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
