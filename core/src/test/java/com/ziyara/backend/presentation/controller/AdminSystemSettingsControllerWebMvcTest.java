package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.SystemSettingsResponse;
import com.ziyara.backend.application.service.SystemSettingsService;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.infrastructure.config.WebMvcConfigurationPropertiesImport;
import com.ziyara.backend.infrastructure.config.LocaleConfig;
import com.ziyara.backend.infrastructure.config.WebMvcSecuritySliceConfiguration;
import com.ziyara.backend.infrastructure.config.SecurityConfig;
import com.ziyara.backend.infrastructure.security.JwtCookieProperties;
import com.ziyara.backend.infrastructure.security.JwtAuthenticationFilter;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminSystemSettingsController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        AdminSystemSettingsControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class AdminSystemSettingsControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    SystemSettingsService systemSettingsService;

    @MockBean
    JwtService jwtService;

    @MockBean
    UserDetailsService userDetailsService;

    @TestConfiguration(proxyBeanMethods = false)
    static class SecurityBeans {

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService,
                                                         SecurityContextRepository securityContextRepository,
                                                         JwtCookieProperties jwtCookieProperties,
                                                         JwtTokenBlocklistService jwtTokenBlocklistService,
                                                         JwtIdleTimeoutService jwtIdleTimeoutService) {
            return new JwtAuthenticationFilter(jwtService, userDetailsService, securityContextRepository,
                    jwtCookieProperties, jwtTokenBlocklistService, jwtIdleTimeoutService);
        }
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001", authorities = "settings:read")
    void getSettings_returns200() throws Exception {
        when(systemSettingsService.getSettings()).thenReturn(SystemSettingsResponse.builder()
                .companyDisplayName("Ziyara")
                .defaultCurrency("USD")
                .maintenanceMode(false)
                .build());

        mockMvc.perform(get("/admin/settings")
                        .header("Authorization", "Bearer test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.companyDisplayName").value("Ziyara"))
                .andExpect(jsonPath("$.data.defaultCurrency").value("USD"))
                .andExpect(jsonPath("$.data.maintenanceMode").value(false));
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001", authorities = "bookings:read")
    void getSettings_forbiddenForWrongRole() throws Exception {
        mockMvc.perform(get("/admin/settings")
                        .header("Authorization", "Bearer test"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001", authorities = "settings:write")
    void putSettings_returns200() throws Exception {
        UUID uid = UUID.fromString("a0000000-0000-0000-0000-000000000001");
        when(systemSettingsService.update(any(), eq(uid))).thenReturn(SystemSettingsResponse.builder()
                .companyDisplayName("Acme")
                .defaultCurrency("SAR")
                .maintenanceMode(true)
                .build());

        mockMvc.perform(patch("/admin/settings").with(csrf())
                        .header("Authorization", "Bearer test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyDisplayName":"Acme","defaultCurrency":"SAR","maintenanceMode":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyDisplayName").value("Acme"))
                .andExpect(jsonPath("$.data.maintenanceMode").value(true));
    }
}
