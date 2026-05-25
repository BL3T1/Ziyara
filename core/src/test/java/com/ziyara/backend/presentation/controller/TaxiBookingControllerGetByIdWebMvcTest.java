package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.TaxiBookingResponse;
import com.ziyara.backend.application.service.TaxiBookingService;
import com.ziyara.backend.domain.enums.TaxiStatus;
import com.ziyara.backend.domain.enums.VehicleType;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.infrastructure.config.WebMvcConfigurationPropertiesImport;
import com.ziyara.backend.infrastructure.config.LocaleConfig;
import com.ziyara.backend.infrastructure.config.WebMvcSecuritySliceConfiguration;
import com.ziyara.backend.infrastructure.config.SecurityConfig;
import com.ziyara.backend.infrastructure.security.JwtCookieProperties;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 4: GET /taxi-bookings/{id} (company staff)
 */
@WebMvcTest(controllers = TaxiBookingController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        TaxiBookingControllerGetByIdWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class TaxiBookingControllerGetByIdWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    TaxiBookingService taxiBookingService;

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
                                                         SecurityContextRepository securityContextRepository,
                                                         JwtCookieProperties jwtCookieProperties,
                                                         JwtTokenBlocklistService jwtTokenBlocklistService) {
            return new JwtAuthenticationFilter(jwtService, userDetailsService, securityContextRepository,
                    jwtCookieProperties, jwtTokenBlocklistService);
        }
    }

    @Test
    @WithMockUser(roles = "SUPPORT_AGENT")
    void getById_returns200() throws Exception {
        UUID id = UUID.fromString("b1000000-0000-4000-8000-000000000001");
        UUID bookingId = UUID.fromString("b2000000-0000-4000-8000-000000000001");
        when(taxiBookingService.getTaxiBooking(eq(id))).thenReturn(TaxiBookingResponse.builder()
                .id(id)
                .bookingId(bookingId)
                .pickupLocation("A")
                .destinationLocation("B")
                .vehicleType(VehicleType.STANDARD)
                .status(TaxiStatus.ASSIGNED)
                .build());

        mockMvc.perform(get("/taxi-bookings/{id}", id).header("Authorization", "Bearer t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pickupLocation").value("A"))
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getById_forbiddenForCustomer() throws Exception {
        UUID id = UUID.fromString("b3000000-0000-4000-8000-000000000001");
        mockMvc.perform(get("/taxi-bookings/{id}", id).header("Authorization", "Bearer t"))
                .andExpect(status().isForbidden());
    }
}
