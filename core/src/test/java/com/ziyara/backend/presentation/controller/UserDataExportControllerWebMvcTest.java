package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.DataExportRequestResponse;
import com.ziyara.backend.application.service.DataExportService;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserDataExportController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        UserDataExportControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class UserDataExportControllerWebMvcTest {

    static final String USER_UUID = "00000000-0000-0000-0000-000000000001";
    static final UUID EXPORT_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Autowired MockMvc mockMvc;

    @MockBean DataExportService dataExportService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    // ── GET /users/me/data-exports ────────────────────────────────────────────

    @Test
    void list_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/users/me/data-exports"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = USER_UUID, authorities = "user:read")
    void list_authenticated_returns200() throws Exception {
        when(dataExportService.list(any())).thenReturn(List.of());

        mockMvc.perform(get("/users/me/data-exports"))
                .andExpect(status().isOk());
    }

    // ── GET /users/me/data-exports/{id} ──────────────────────────────────────

    @Test
    void getOne_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/users/me/data-exports/" + EXPORT_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = USER_UUID, authorities = "user:read")
    void getOne_authenticated_returns200() throws Exception {
        DataExportRequestResponse response = DataExportRequestResponse.builder()
                .id(EXPORT_ID)
                .build();
        when(dataExportService.getForUser(eq(EXPORT_ID), any())).thenReturn(response);

        mockMvc.perform(get("/users/me/data-exports/" + EXPORT_ID))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USER_UUID, authorities = "user:read")
    void getOne_notFound_returns404() throws Exception {
        when(dataExportService.getForUser(any(), any()))
                .thenThrow(new IllegalArgumentException("Not found"));

        mockMvc.perform(get("/users/me/data-exports/" + EXPORT_ID))
                .andExpect(status().isNotFound());
    }

    // ── GET /users/me/data-exports/{id}/download ──────────────────────────────

    @Test
    void download_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/users/me/data-exports/" + EXPORT_ID + "/download"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = USER_UUID, authorities = "user:read")
    void download_authenticated_completedExport_returns200() throws Exception {
        when(dataExportService.downloadPayloadForUser(any(), any()))
                .thenReturn("{\"userId\":\"test\"}".getBytes());

        mockMvc.perform(get("/users/me/data-exports/" + EXPORT_ID + "/download"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USER_UUID, authorities = "user:read")
    void download_pendingExport_returns409() throws Exception {
        when(dataExportService.downloadPayloadForUser(any(), any()))
                .thenThrow(new IllegalStateException("Export not ready"));

        mockMvc.perform(get("/users/me/data-exports/" + EXPORT_ID + "/download"))
                .andExpect(status().isConflict());
    }

    // ── POST /users/me/data-exports ───────────────────────────────────────────

    @Test
    void request_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/users/me/data-exports"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = USER_UUID, authorities = "user:read")
    void request_authenticated_returns200() throws Exception {
        DataExportRequestResponse response = DataExportRequestResponse.builder()
                .id(EXPORT_ID)
                .build();
        when(dataExportService.requestExport(any())).thenReturn(response);

        mockMvc.perform(post("/users/me/data-exports"))
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
