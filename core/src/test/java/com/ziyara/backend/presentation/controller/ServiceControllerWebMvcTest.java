package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.ServiceResponse;
import com.ziyara.backend.application.query.ServiceQueryHandler;
import com.ziyara.backend.application.service.HotelRoomService;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.application.service.RestaurantMenuService;
import com.ziyara.backend.application.service.ServiceImageService;
import com.ziyara.backend.application.service.ServiceService;
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

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ServiceController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        ServiceControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class ServiceControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockBean ServiceService serviceService;
    @MockBean ServiceQueryHandler serviceQueryHandler;
    @MockBean ServiceImageService serviceImageService;
    @MockBean RestaurantMenuService restaurantMenuService;
    @MockBean HotelRoomService hotelRoomService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    private static final UUID SERVICE_ID = UUID.randomUUID();

    // ── GET /services (public/open) ────────────────────────────────────────

    @Test
    void listServices_noAuth_returns200() throws Exception {
        when(serviceQueryHandler.findPage(anyInt(), anyInt(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/services"))
                .andExpect(status().isOk());
    }

    @Test
    void getServiceById_noAuth_returns200() throws Exception {
        ServiceResponse response = ServiceResponse.builder().id(SERVICE_ID).build();
        when(serviceQueryHandler.findById(SERVICE_ID)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/services/{id}", SERVICE_ID))
                .andExpect(status().isOk());
    }

    @Test
    void getServiceById_notFound_returns404() throws Exception {
        when(serviceQueryHandler.findById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/services/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ── POST /services ────────────────────────────────────────────────────────

    @Test
    void createService_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Service\",\"type\":\"HOTEL\",\"providerId\":\"00000000-0000-0000-0000-000000000001\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "bookings:read")
    void createService_withCompanyStaff_returns201() throws Exception {
        ServiceResponse created = ServiceResponse.builder().id(SERVICE_ID).build();
        when(serviceService.create(any())).thenReturn(created);

        mockMvc.perform(post("/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hotel Service\",\"type\":\"HOTEL\",\"providerId\":\"00000000-0000-0000-0000-000000000001\",\"city\":\"Dubai\",\"country\":\"UAE\"}"))
                .andExpect(status().isCreated());
    }

    // ── POST /services/{id}/approve ────────────────────────────────────────────

    @Test
    void approveService_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/services/{id}/approve", SERVICE_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "services:publish")
    void approveService_withPermission_returns200() throws Exception {
        ServiceResponse approved = ServiceResponse.builder().id(SERVICE_ID).build();
        when(serviceService.approve(SERVICE_ID)).thenReturn(approved);

        mockMvc.perform(post("/services/{id}/approve", SERVICE_ID))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "bookings:read")
    void approveService_withoutPublishPermission_returns403() throws Exception {
        mockMvc.perform(post("/services/{id}/approve", SERVICE_ID))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /services/{id} ──────────────────────────────────────────────────

    @Test
    void deleteService_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/services/{id}", SERVICE_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "bookings:read")
    void deleteService_withCompanyStaff_returns200() throws Exception {
        mockMvc.perform(delete("/services/{id}", SERVICE_ID))
                .andExpect(status().isOk());
    }

    // ── PATCH /services/{id} ───────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "bookings:read")
    void updateService_withCompanyStaff_returns200() throws Exception {
        ServiceResponse updated = ServiceResponse.builder().id(SERVICE_ID).build();
        when(serviceService.update(eq(SERVICE_ID), any())).thenReturn(updated);

        mockMvc.perform(patch("/services/{id}", SERVICE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Service\"}"))
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
