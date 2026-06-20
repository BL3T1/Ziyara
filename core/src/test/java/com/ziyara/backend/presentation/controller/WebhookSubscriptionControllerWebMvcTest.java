package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.application.service.WebhookService;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WebhookSubscriptionController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        WebhookSubscriptionControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class WebhookSubscriptionControllerWebMvcTest {

    static final UUID WEBHOOK_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Autowired MockMvc mockMvc;

    @MockBean WebhookService webhookService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    // ── GET /admin/webhooks ───────────────────────────────────────────────────

    @Test
    void list_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/admin/webhooks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "webhooks:read")
    void list_withCompanyStaff_returns200() throws Exception {
        when(webhookService.list(anyInt(), anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/admin/webhooks"))
                .andExpect(status().isOk());
    }

    // ── POST /admin/webhooks ──────────────────────────────────────────────────

    @Test
    void create_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/admin/webhooks").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"hook\",\"url\":\"https://example.com\",\"events\":[\"booking.created\"]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "webhooks:read")
    void create_withCompanyStaff_returns201() throws Exception {
        when(webhookService.create(any())).thenReturn(null);

        mockMvc.perform(post("/admin/webhooks").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"hook\",\"url\":\"https://example.com\",\"events\":[\"booking.created\"]}"))
                .andExpect(status().isCreated());
    }

    // ── DELETE /admin/webhooks/{id} ───────────────────────────────────────────

    @Test
    void delete_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/admin/webhooks/" + WEBHOOK_ID).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "webhooks:read")
    void delete_withCompanyStaff_returns200() throws Exception {
        doNothing().when(webhookService).delete(any());

        mockMvc.perform(delete("/admin/webhooks/" + WEBHOOK_ID).with(csrf()))
                .andExpect(status().isOk());
    }

    // ── PATCH /admin/webhooks/{id}/active ─────────────────────────────────────

    @Test
    void setActive_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch("/admin/webhooks/" + WEBHOOK_ID + "/active").with(csrf())
                        .param("active", "true"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "webhooks:read")
    void setActive_withCompanyStaff_returns200() throws Exception {
        doNothing().when(webhookService).setActive(any(), anyBoolean());

        mockMvc.perform(patch("/admin/webhooks/" + WEBHOOK_ID + "/active").with(csrf())
                        .param("active", "false"))
                .andExpect(status().isOk());
    }

    // ── POST /admin/webhooks/{id}/ping ────────────────────────────────────────

    @Test
    void ping_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/admin/webhooks/" + WEBHOOK_ID + "/ping").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "webhooks:read")
    void ping_withCompanyStaff_returns200() throws Exception {
        doNothing().when(webhookService).ping(any());

        mockMvc.perform(post("/admin/webhooks/" + WEBHOOK_ID + "/ping").with(csrf()))
                .andExpect(status().isOk());
    }

    // ── GET /admin/webhooks/events ────────────────────────────────────────────

    @Test
    void supportedEvents_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/admin/webhooks/events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "webhooks:read")
    void supportedEvents_withCompanyStaff_returns200() throws Exception {
        when(webhookService.getSupportedEvents()).thenReturn(List.of("booking.created", "payment.completed"));

        mockMvc.perform(get("/admin/webhooks/events"))
                .andExpect(status().isOk());
    }

    // ── GET /admin/webhooks/{id}/deliveries ───────────────────────────────────

    @Test
    void deliveries_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/admin/webhooks/" + WEBHOOK_ID + "/deliveries"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "webhooks:read")
    void deliveries_withCompanyStaff_returns200() throws Exception {
        when(webhookService.listDeliveries(any(), anyInt(), anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/admin/webhooks/" + WEBHOOK_ID + "/deliveries"))
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
