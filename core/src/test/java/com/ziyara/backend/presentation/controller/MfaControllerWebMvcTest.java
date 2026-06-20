package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.application.service.UserMfaService;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MfaController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        MfaControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class MfaControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockBean UserMfaService userMfaService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    private static final String USER_ID = UUID.randomUUID().toString();

    // ── POST /users/me/mfa/enroll/start ───────────────────────────────────────

    @Test
    void enrollStart_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/users/me/mfa/enroll/start").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "not-a-uuid")
    void enrollStart_authenticatedNonUuidUsername_returns200WithError() throws Exception {
        mockMvc.perform(post("/users/me/mfa/enroll/start").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001", authorities = "user:read")
    void enrollStart_authenticated_returns200() throws Exception {
        UserMfaService.MfaEnrollmentStartResult result =
                new UserMfaService.MfaEnrollmentStartResult("BASE32SECRET", "otpauth://totp/Ziyara:mfa-user");
        when(userMfaService.startEnrollment(any())).thenReturn(result);

        mockMvc.perform(post("/users/me/mfa/enroll/start").with(csrf()))
                .andExpect(status().isOk());
    }

    // ── POST /users/me/mfa/enroll/confirm ─────────────────────────────────────

    @Test
    void enrollConfirm_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/users/me/mfa/enroll/confirm").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"123456\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001", authorities = "user:read")
    void enrollConfirm_validCode_returns200() throws Exception {
        mockMvc.perform(post("/users/me/mfa/enroll/confirm").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"123456\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001", authorities = "user:read")
    void enrollConfirm_missingCode_returns400() throws Exception {
        mockMvc.perform(post("/users/me/mfa/enroll/confirm").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── POST /users/me/mfa/disable ────────────────────────────────────────────

    @Test
    void disableMfa_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/users/me/mfa/disable").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001", authorities = "user:read")
    void disableMfa_authenticated_returns200() throws Exception {
        mockMvc.perform(post("/users/me/mfa/disable").with(csrf()))
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
