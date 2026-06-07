package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.NotificationInboxResponse;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.application.service.NotificationService;
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
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = NotificationController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        NotificationControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class NotificationControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockBean NotificationService notificationService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    private static final UUID USER_ID = UUID.randomUUID();

    // ── GET /notifications — requires JWT extraction ───────────────────────────

    @Test
    void getMyNotifications_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "user:read")
    void getMyNotifications_authenticated_returns200() throws Exception {
        when(jwtService.extractUserId("test-token")).thenReturn(USER_ID.toString());
        NotificationInboxResponse inbox = NotificationInboxResponse.builder()
                .notifications(Page.empty())
                .unreadCount(0L)
                .build();
        when(notificationService.getUserNotificationsInbox(eq(USER_ID), anyInt(), anyInt()))
                .thenReturn(inbox);

        mockMvc.perform(get("/notifications")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk());
    }

    // ── GET /notifications/me ──────────────────────────────────────────────────

    @Test
    void getMyNotificationsAlias_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/notifications/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "user:read")
    void getMyNotificationsAlias_authenticated_returns200() throws Exception {
        when(jwtService.extractUserId("test-token")).thenReturn(USER_ID.toString());
        NotificationInboxResponse inbox = NotificationInboxResponse.builder()
                .notifications(Page.empty())
                .unreadCount(0L)
                .build();
        when(notificationService.getUserNotificationsInbox(any(), anyInt(), anyInt()))
                .thenReturn(inbox);

        mockMvc.perform(get("/notifications/me")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk());
    }

    // ── GET /notifications/me/unread-count ────────────────────────────────────

    @Test
    @WithMockUser(authorities = "user:read")
    void getUnreadCount_authenticated_returns200() throws Exception {
        when(jwtService.extractUserId("test-token")).thenReturn(USER_ID.toString());
        when(notificationService.countUnread(USER_ID)).thenReturn(3L);

        mockMvc.perform(get("/notifications/me/unread-count")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk());
    }

    // ── PATCH /notifications/{id}/read ────────────────────────────────────────

    @Test
    void markAsRead_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch("/notifications/{id}/read", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "user:read")
    void markAsRead_authenticated_returns200() throws Exception {
        UUID notifId = UUID.randomUUID();
        when(jwtService.extractUserId("test-token")).thenReturn(USER_ID.toString());

        mockMvc.perform(patch("/notifications/{id}/read", notifId)
                        .header("Authorization", "Bearer test-token"))
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
