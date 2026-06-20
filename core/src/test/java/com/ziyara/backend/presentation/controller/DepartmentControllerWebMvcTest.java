package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.DepartmentResponse;
import com.ziyara.backend.application.service.DepartmentService;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DepartmentController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        DepartmentControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class DepartmentControllerWebMvcTest {

    static final UUID DEPT_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @Autowired MockMvc mockMvc;

    @MockBean DepartmentService departmentService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    // ── GET /departments ──────────────────────────────────────────────────────

    @Test
    void list_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/departments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "departments:read")
    void list_withCompanyStaff_returns200() throws Exception {
        when(departmentService.getAllDepartments()).thenReturn(List.of());

        mockMvc.perform(get("/departments"))
                .andExpect(status().isOk());
    }

    // ── GET /departments/{id} ─────────────────────────────────────────────────

    @Test
    void get_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/departments/" + DEPT_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "departments:read")
    void get_withCompanyStaff_returns200() throws Exception {
        DepartmentResponse response = DepartmentResponse.builder()
                .id(DEPT_ID)
                .name("Engineering")
                .build();
        when(departmentService.getDepartment(DEPT_ID)).thenReturn(response);

        mockMvc.perform(get("/departments/" + DEPT_ID))
                .andExpect(status().isOk());
    }

    // ── POST /departments ─────────────────────────────────────────────────────

    @Test
    void create_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/departments").with(csrf()).param("name", "HR"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "departments:read")
    void create_withCompanyStaff_returns200() throws Exception {
        DepartmentResponse response = DepartmentResponse.builder()
                .id(DEPT_ID)
                .name("HR")
                .build();
        when(departmentService.createDepartment(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/departments").with(csrf()).param("name", "HR"))
                .andExpect(status().isOk());
    }

    // ── PATCH /departments/{id} ───────────────────────────────────────────────

    @Test
    void update_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch("/departments/" + DEPT_ID).with(csrf()).param("name", "Updated"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "departments:read")
    void update_withCompanyStaff_returns200() throws Exception {
        DepartmentResponse response = DepartmentResponse.builder()
                .id(DEPT_ID)
                .name("Updated")
                .build();
        when(departmentService.updateDepartment(any(), any(), any(), any())).thenReturn(response);

        mockMvc.perform(patch("/departments/" + DEPT_ID).with(csrf()).param("name", "Updated"))
                .andExpect(status().isOk());
    }

    // ── DELETE /departments/{id} ──────────────────────────────────────────────

    @Test
    void delete_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/departments/" + DEPT_ID).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "departments:read")
    void delete_withCompanyStaff_returns200() throws Exception {
        doNothing().when(departmentService).deleteDepartment(any());

        mockMvc.perform(delete("/departments/" + DEPT_ID).with(csrf()))
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
