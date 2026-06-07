package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.BookingResponse;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.infrastructure.config.LocaleConfig;
import com.ziyara.backend.infrastructure.config.SecurityConfig;
import com.ziyara.backend.infrastructure.config.WebMvcConfigurationPropertiesImport;
import com.ziyara.backend.infrastructure.config.WebMvcSecuritySliceConfiguration;
import com.ziyara.backend.infrastructure.security.JwtAuthenticationFilter;
import com.ziyara.backend.infrastructure.security.JwtCookieProperties;
import com.ziyara.backend.infrastructure.security.JwtIdleTimeoutService;
import com.ziyara.backend.infrastructure.security.JwtService;
import com.ziyara.backend.modules.booking.api.BookingServiceApi;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BookingController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        BookingControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class BookingControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockBean BookingServiceApi bookingService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    private static final UUID USER_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private static final UUID BOOKING_ID = UUID.randomUUID();
    private static final String AUTH_HEADER = "Bearer test-token";

    // ── Unauthenticated → 401 ────────────────────────────────────────────────

    @Test
    void getBookings_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/bookings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createBooking_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceId\":\"00000000-0000-0000-0000-000000000001\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /bookings/admin ───────────────────────────────────────────────────

    @Test
    void listAllAdmin_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/bookings/admin"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "bookings:read")
    void listAllAdmin_withPermission_returns200() throws Exception {
        when(bookingService.listAllAdmin(any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/bookings/admin"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "users:read")
    void listAllAdmin_withoutBookingsRead_returns403() throws Exception {
        mockMvc.perform(get("/bookings/admin"))
                .andExpect(status().isForbidden());
    }

    // ── GET /bookings ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001")
    void getBookings_authenticated_returns200() throws Exception {
        when(jwtService.extractUserId("test-token")).thenReturn(USER_ID.toString());
        when(bookingService.getAllBookings(eq(USER_ID), anyBoolean(), any(), anyBoolean(), anyInt(), anyInt()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/bookings")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk());
    }

    // ── POST /bookings ────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001")
    void createBooking_authenticated_returns201() throws Exception {
        when(jwtService.extractUserId("test-token")).thenReturn(USER_ID.toString());
        BookingResponse booking = BookingResponse.builder().id(BOOKING_ID).build();
        when(bookingService.createBooking(eq(USER_ID), any())).thenReturn(booking);

        mockMvc.perform(post("/bookings")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceId\":\"00000000-0000-0000-0000-000000000001\",\"checkInDate\":\"2025-12-01\",\"checkOutDate\":\"2025-12-05\",\"guests\":2}"))
                .andExpect(status().isCreated());
    }

    // ── POST /bookings/{id}/reject ────────────────────────────────────────────

    @Test
    void rejectBooking_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/bookings/{id}/reject", BOOKING_ID)
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001", authorities = "bookings:write")
    void rejectBooking_withPermission_returns200() throws Exception {
        when(jwtService.extractUserId("test-token")).thenReturn(USER_ID.toString());
        BookingResponse booking = BookingResponse.builder().id(BOOKING_ID).build();
        when(bookingService.rejectBooking(eq(BOOKING_ID), eq(USER_ID), any())).thenReturn(booking);

        mockMvc.perform(post("/bookings/{id}/reject", BOOKING_ID)
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001", authorities = "users:read")
    void rejectBooking_withoutPermission_returns403() throws Exception {
        mockMvc.perform(post("/bookings/{id}/reject", BOOKING_ID)
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isForbidden());
    }

    // ── POST /bookings/{id}/cancel ────────────────────────────────────────────

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001")
    void cancelBooking_authenticated_returns200() throws Exception {
        when(jwtService.extractUserId("test-token")).thenReturn(USER_ID.toString());
        BookingResponse booking = BookingResponse.builder().id(BOOKING_ID).build();
        when(bookingService.cancelBooking(eq(BOOKING_ID), eq(USER_ID), anyBoolean(), any()))
                .thenReturn(booking);

        mockMvc.perform(post("/bookings/{id}/cancel", BOOKING_ID)
                        .header("Authorization", AUTH_HEADER))
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
