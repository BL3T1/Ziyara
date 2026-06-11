package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.service.AuthService;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.application.service.LoginRateLimitService;
import com.ziyara.backend.application.service.SecurityAlertService;
import com.ziyara.backend.application.service.SecurityEventService;
import com.ziyara.backend.infrastructure.config.LocaleConfig;
import com.ziyara.backend.infrastructure.config.SecurityConfig;
import com.ziyara.backend.infrastructure.config.WebMvcConfigurationPropertiesImport;
import com.ziyara.backend.infrastructure.config.WebMvcSecuritySliceConfiguration;
import com.ziyara.backend.infrastructure.security.JwtAuthenticationFilter;
import com.ziyara.backend.infrastructure.security.JwtCookieProperties;
import com.ziyara.backend.infrastructure.security.JwtIdleTimeoutService;
import com.ziyara.backend.infrastructure.security.JwtService;
import com.ziyara.backend.application.dto.AuthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        AuthControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class AuthControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockBean AuthService authService;
    @MockBean LoginRateLimitService loginRateLimitService;
    @MockBean SecurityEventService securityEventService;
    @MockBean SecurityAlertService securityAlertService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    // ── POST /auth/register ──────────────────────────────────────────────────

    @Test
    void register_validBody_returns201() throws Exception {
        mockMvc.perform(post("/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"Password123!\",\"firstName\":\"John\",\"lastName\":\"Doe\",\"role\":\"CUSTOMER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void register_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── POST /auth/login ─────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access-jwt")
                .refreshToken("refresh-jwt")
                .userId(UUID.randomUUID())
                .email("user@example.com")
                .build();
        when(loginRateLimitService.allow(anyString(), anyString())).thenReturn(true);
        when(authService.authenticate(any(), anyString())).thenReturn(authResponse);

        mockMvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-jwt"));
    }

    @Test
    void login_rateLimited_returns429() throws Exception {
        when(loginRateLimitService.allow(anyString(), anyString())).thenReturn(false);

        mockMvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"Password123!\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        when(loginRateLimitService.allow(anyString(), anyString())).thenReturn(true);
        when(authService.authenticate(any(), anyString()))
                .thenThrow(new AuthService.AuthenticationException("Invalid email or password"));

        mockMvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bad@example.com\",\"password\":\"wrongp\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /auth/logout ────────────────────────────────────────────────────

    @Test
    void logout_returns200() throws Exception {
        mockMvc.perform(post("/auth/logout").with(csrf())
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk());
    }

    // ── POST /auth/refresh ───────────────────────────────────────────────────

    @Test
    void refresh_noToken_returns401() throws Exception {
        mockMvc.perform(post("/auth/refresh").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_withHeader_callsService() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("new-access")
                .refreshToken("new-refresh")
                .build();
        when(authService.refreshToken("valid-refresh")).thenReturn(authResponse);

        mockMvc.perform(post("/auth/refresh").with(csrf())
                        .header("Refresh-Token", "valid-refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access"));
    }

    // ── POST /auth/password/forgot ───────────────────────────────────────────

    @Test
    void forgotPassword_returns200() throws Exception {
        mockMvc.perform(post("/auth/password/forgot").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\"}"))
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
