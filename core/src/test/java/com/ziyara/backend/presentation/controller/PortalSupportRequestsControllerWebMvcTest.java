package com.ziyara.backend.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziyara.backend.application.dto.request.CreatePortalSupportRequest;
import com.ziyara.backend.application.dto.response.PortalSupportRequestResponse;
import com.ziyara.backend.application.dto.response.ServiceProviderResponse;
import com.ziyara.backend.application.service.PortalSupportRequestService;
import com.ziyara.backend.application.service.ServiceProviderService;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PortalSupportRequestsController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        PortalSupportRequestsControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class PortalSupportRequestsControllerWebMvcTest {

    private static final UUID PORTAL_USER_ID = UUID.fromString("c1000000-0000-4000-8000-000000000001");
    private static final UUID PROVIDER_ID = UUID.fromString("c2000000-0000-4000-8000-000000000001");
    private static final UUID REQ_ID = UUID.fromString("c3000000-0000-4000-8000-000000000001");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    PortalSupportRequestService portalSupportRequestService;

    @MockBean
    ServiceProviderService providerService;

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

    private void stubCurrentProvider() {
        when(providerService.getProviderByUserId(PORTAL_USER_ID)).thenReturn(Optional.of(
                ServiceProviderResponse.builder()
                        .id(PROVIDER_ID)
                        .userId(PORTAL_USER_ID)
                        .name("Test Org")
                        .build()));
    }

    @Test
    @WithMockUser(username = "c1000000-0000-4000-8000-000000000001", roles = "PROVIDER_MANAGER")
    void list_returns200() throws Exception {
        stubCurrentProvider();
        PortalSupportRequestResponse row = PortalSupportRequestResponse.builder()
                .id(REQ_ID)
                .subject("Help")
                .body("Details")
                .userId(PORTAL_USER_ID)
                .createdAt(Instant.parse("2026-01-15T10:00:00Z"))
                .build();
        when(portalSupportRequestService.listForProvider(PROVIDER_ID)).thenReturn(List.of(row));

        mockMvc.perform(get("/portal/support-requests").header("Authorization", "Bearer t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].subject").value("Help"));
    }

    @Test
    @WithMockUser(username = "c1000000-0000-4000-8000-000000000001", roles = "PROVIDER_MANAGER")
    void create_returns201() throws Exception {
        stubCurrentProvider();
        PortalSupportRequestResponse created = PortalSupportRequestResponse.builder()
                .id(REQ_ID)
                .subject("Billing")
                .body("Question")
                .userId(PORTAL_USER_ID)
                .createdAt(Instant.parse("2026-01-15T10:00:00Z"))
                .build();
        when(portalSupportRequestService.create(eq(PROVIDER_ID), eq(PORTAL_USER_ID), any(CreatePortalSupportRequest.class)))
                .thenReturn(created);

        CreatePortalSupportRequest body = CreatePortalSupportRequest.builder()
                .subject("Billing")
                .body("Question")
                .build();

        mockMvc.perform(post("/portal/support-requests")
                        .header("Authorization", "Bearer t")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.subject").value("Billing"));

        verify(portalSupportRequestService).create(eq(PROVIDER_ID), eq(PORTAL_USER_ID), any(CreatePortalSupportRequest.class));
    }

    @Test
    @WithMockUser(roles = "SUPPORT_AGENT")
    void list_forbiddenForCompanyStaff() throws Exception {
        mockMvc.perform(get("/portal/support-requests").header("Authorization", "Bearer t"))
                .andExpect(status().isForbidden());
    }
}
