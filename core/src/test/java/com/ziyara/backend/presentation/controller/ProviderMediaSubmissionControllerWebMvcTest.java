package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.ProviderMediaSubmissionResponse;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.application.service.ProviderMediaSubmissionService;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProviderMediaSubmissionController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        ProviderMediaSubmissionControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class ProviderMediaSubmissionControllerWebMvcTest {

    static final String USER_UUID = "00000000-0000-0000-0000-000000000001";
    static final UUID SUBMISSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Autowired MockMvc mockMvc;

    @MockBean ProviderMediaSubmissionService submissionService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    // ── GET /admin/media-submissions ──────────────────────────────────────────

    @Test
    void list_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/admin/media-submissions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "bookings:read")
    void list_withoutMediaApprovePermission_returns403() throws Exception {
        mockMvc.perform(get("/admin/media-submissions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = USER_UUID, authorities = "media_submissions:approve")
    void list_withMediaApprovePermission_returns200() throws Exception {
        when(submissionService.getPendingSubmissions()).thenReturn(List.of());

        mockMvc.perform(get("/admin/media-submissions"))
                .andExpect(status().isOk());
    }

    // ── POST /admin/media-submissions/{id}/approve ────────────────────────────

    @Test
    void approve_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/admin/media-submissions/" + SUBMISSION_ID + "/approve").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "bookings:read")
    void approve_withoutPermission_returns403() throws Exception {
        mockMvc.perform(post("/admin/media-submissions/" + SUBMISSION_ID + "/approve").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = USER_UUID, authorities = "media_submissions:approve")
    void approve_withPermission_returns200() throws Exception {
        ProviderMediaSubmissionResponse response = ProviderMediaSubmissionResponse.builder()
                .id(SUBMISSION_ID)
                .build();
        when(submissionService.approve(any(), any())).thenReturn(response);

        mockMvc.perform(post("/admin/media-submissions/" + SUBMISSION_ID + "/approve").with(csrf()))
                .andExpect(status().isOk());
    }

    // ── POST /admin/media-submissions/{id}/reject ─────────────────────────────

    @Test
    void reject_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/admin/media-submissions/" + SUBMISSION_ID + "/reject").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = USER_UUID, authorities = "media_submissions:approve")
    void reject_withPermission_returns200() throws Exception {
        ProviderMediaSubmissionResponse response = ProviderMediaSubmissionResponse.builder()
                .id(SUBMISSION_ID)
                .build();
        when(submissionService.reject(any(), any(), isNull())).thenReturn(response);

        mockMvc.perform(post("/admin/media-submissions/" + SUBMISSION_ID + "/reject").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
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
