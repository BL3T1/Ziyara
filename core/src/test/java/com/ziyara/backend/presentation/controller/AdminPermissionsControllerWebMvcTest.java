package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.service.AdminActivityLogService;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.infrastructure.config.LocaleConfig;
import com.ziyara.backend.infrastructure.config.SecurityConfig;
import com.ziyara.backend.infrastructure.config.WebMvcConfigurationPropertiesImport;
import com.ziyara.backend.infrastructure.config.WebMvcSecuritySliceConfiguration;
import com.ziyara.backend.infrastructure.security.JwtAuthenticationFilter;
import com.ziyara.backend.infrastructure.security.JwtCookieProperties;
import com.ziyara.backend.infrastructure.security.JwtIdleTimeoutService;
import com.ziyara.backend.infrastructure.security.JwtService;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminPermissionsController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        AdminPermissionsControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class AdminPermissionsControllerWebMvcTest {

    static final UUID ROLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @Autowired MockMvc mockMvc;

    // DSLContext provided as deep-stub by SecurityBeans to handle jOOQ fluent chain
    @Autowired DSLContext dsl;

    @MockBean AdminActivityLogService activityLogService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    // ── GET /admin/permissions/matrix ─────────────────────────────────────────

    @Test
    void getMatrix_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/admin/permissions/matrix"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "audit:read")
    void getMatrix_withoutRolesRead_returns403() throws Exception {
        mockMvc.perform(get("/admin/permissions/matrix"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "roles:read")
    void getMatrix_withRolesRead_returns200() throws Exception {
        // Deep-stub DSLContext: select().from().orderBy().fetchMaps() → mock List (serializes as [])
        mockMvc.perform(get("/admin/permissions/matrix"))
                .andExpect(status().isOk());
    }

    // ── POST /admin/permissions/matrix ────────────────────────────────────────

    @Test
    void upsertPermission_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/admin/permissions/matrix").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleId\":\"" + ROLE_ID + "\",\"module\":\"booking\",\"action\":\"read\",\"granted\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "roles:read")
    void upsertPermission_withoutRolesWrite_returns403() throws Exception {
        mockMvc.perform(post("/admin/permissions/matrix").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleId\":\"" + ROLE_ID + "\",\"module\":\"booking\",\"action\":\"read\",\"granted\":true}")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "roles:write")
    void upsertPermission_withRolesWrite_returns200() throws Exception {
        when(jwtService.extractUserId(any())).thenReturn(ROLE_ID.toString());

        mockMvc.perform(post("/admin/permissions/matrix").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleId\":\"" + ROLE_ID + "\",\"module\":\"booking\",\"action\":\"read\",\"granted\":true}")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isOk());
    }

    // ── PUT /admin/permissions/roles/{roleId} ─────────────────────────────────

    @Test
    void updateRolePermissions_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/admin/permissions/roles/" + ROLE_ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "roles:read")
    void updateRolePermissions_withoutRolesWrite_returns403() throws Exception {
        mockMvc.perform(put("/admin/permissions/roles/" + ROLE_ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "roles:write")
    void updateRolePermissions_withRolesWrite_returns200() throws Exception {
        when(jwtService.extractUserId(any())).thenReturn(ROLE_ID.toString());

        mockMvc.perform(put("/admin/permissions/roles/" + ROLE_ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isOk());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SecurityBeans {
        @Bean
        DSLContext dsl() {
            // RETURNS_DEEP_STUBS prevents NPE in jOOQ fluent chains
            return mock(DSLContext.class, Answers.RETURNS_DEEP_STUBS);
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
