package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.AuditLogResponse;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.infrastructure.config.LocaleConfig;
import com.ziyara.backend.infrastructure.config.SecurityConfig;
import com.ziyara.backend.infrastructure.config.WebMvcConfigurationPropertiesImport;
import com.ziyara.backend.infrastructure.config.WebMvcSecuritySliceConfiguration;
import com.ziyara.backend.infrastructure.security.JwtAuthenticationFilter;
import com.ziyara.backend.infrastructure.security.JwtCookieProperties;
import com.ziyara.backend.infrastructure.security.JwtIdleTimeoutService;
import com.ziyara.backend.infrastructure.security.JwtService;
import com.ziyara.backend.modules.sys.api.AuditServiceApi;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuditLogController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        AuditLogControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class AuditLogControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockBean AuditServiceApi auditLogService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    @Test
    void getAuditLogs_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/audit-logs")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "audit:read")
    void getAuditLogs_withPermission_returns200() throws Exception {
        when(auditLogService.getRecentLogs(anyInt(), any())).thenReturn(List.of());
        mockMvc.perform(get("/audit-logs")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "bookings:read")
    void getAuditLogs_withoutAuditRead_returns403() throws Exception {
        mockMvc.perform(get("/audit-logs")).andExpect(status().isForbidden());
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
