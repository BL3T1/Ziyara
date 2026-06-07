package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.RoleResponse;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.infrastructure.config.WebMvcConfigurationPropertiesImport;
import com.ziyara.backend.infrastructure.config.LocaleConfig;
import com.ziyara.backend.infrastructure.config.WebMvcSecuritySliceConfiguration;
import com.ziyara.backend.infrastructure.config.SecurityConfig;
import com.ziyara.backend.infrastructure.security.JwtCookieProperties;
import com.ziyara.backend.infrastructure.security.JwtAuthenticationFilter;
import com.ziyara.backend.infrastructure.security.JwtIdleTimeoutService;
import com.ziyara.backend.infrastructure.security.JwtService;
import com.ziyara.backend.modules.sys.api.RoleServiceApi;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RoleManagementController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        RoleManagementControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class RoleManagementControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    RoleServiceApi roleManagementService;

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
                                                         JwtTokenBlocklistService jwtTokenBlocklistService,
                                                         JwtIdleTimeoutService jwtIdleTimeoutService) {
            return new JwtAuthenticationFilter(jwtService, userDetailsService, securityContextRepository,
                    jwtCookieProperties, jwtTokenBlocklistService, jwtIdleTimeoutService);
        }
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001", roles = "SUPER_ADMIN")
    void putRolePermissions_superAdmin_ok() throws Exception {
        UUID roleId = UUID.fromString("c0000000-0000-0000-0000-000000000001");
        UUID actor = UUID.fromString("a0000000-0000-0000-0000-000000000001");
        RoleResponse body = RoleResponse.builder()
                .id(roleId)
                .name("Super Admin")
                .systemRole(true)
                .permissionIds(List.of())
                .userCount(0)
                .build();
        when(roleManagementService.updateRolePermissions(eq(roleId), any(), eq(actor))).thenReturn(body);

        mockMvc.perform(put("/roles/{id}/permissions", roleId)
                        .header("Authorization", "Bearer test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissionIds\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.systemRole").value(true));
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001", roles = "HR_MANAGER")
    void putRolePermissions_nonSuperAdmin_forbidden() throws Exception {
        UUID roleId = UUID.fromString("c0000000-0000-0000-0000-000000000002");

        mockMvc.perform(put("/roles/{id}/permissions", roleId)
                        .header("Authorization", "Bearer test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissionIds\":[]}"))
                .andExpect(status().isForbidden());
    }
}
