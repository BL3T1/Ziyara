package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.request.CreateEmployeeRequest;
import com.ziyara.backend.application.dto.response.EmployeeResponse;
import com.ziyara.backend.application.service.EmployeeService;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EmployeeController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        EmployeeControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class EmployeeControllerWebMvcTest {

    static final UUID EMP_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

    @Autowired MockMvc mockMvc;

    @MockBean EmployeeService employeeService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    // ── GET /employees ────────────────────────────────────────────────────────

    @Test
    void list_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/employees"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "employees:read")
    void list_allStatus_returns200() throws Exception {
        when(employeeService.getAllEmployees()).thenReturn(List.of());

        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "employees:read")
    void list_activeStatus_callsGetActive() throws Exception {
        when(employeeService.getActiveEmployees()).thenReturn(List.of());

        mockMvc.perform(get("/employees").param("status", "ACTIVE"))
                .andExpect(status().isOk());
    }

    // ── GET /employees/{id} ───────────────────────────────────────────────────

    @Test
    void get_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/employees/" + EMP_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "employees:read")
    void get_withCompanyStaff_returns200() throws Exception {
        EmployeeResponse response = EmployeeResponse.builder()
                .id(EMP_ID)
                .build();
        when(employeeService.getEmployee(EMP_ID)).thenReturn(response);

        mockMvc.perform(get("/employees/" + EMP_ID))
                .andExpect(status().isOk());
    }

    // ── POST /employees ───────────────────────────────────────────────────────

    @Test
    void onboard_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/employees").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + EMP_ID + "\",\"name\":\"John\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "employees:read")
    void onboard_withCompanyStaff_returns201() throws Exception {
        EmployeeResponse response = EmployeeResponse.builder().id(EMP_ID).build();
        when(employeeService.createEmployee(any())).thenReturn(response);

        mockMvc.perform(post("/employees").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + EMP_ID + "\",\"employeeId\":\"EMP001\"}"))
                .andExpect(status().isCreated());
    }

    // ── PATCH /employees/{id} ─────────────────────────────────────────────────

    @Test
    void update_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch("/employees/" + EMP_ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "employees:read")
    void update_withCompanyStaff_returns200() throws Exception {
        EmployeeResponse response = EmployeeResponse.builder().id(EMP_ID).build();
        when(employeeService.updateEmployee(any(), any())).thenReturn(response);

        mockMvc.perform(patch("/employees/" + EMP_ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    // ── DELETE /employees/{id} ────────────────────────────────────────────────

    @Test
    void offboard_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/employees/" + EMP_ID).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", authorities = "employees:read")
    void offboard_withCompanyStaff_returns200() throws Exception {
        // resolveActorId tolerates non-UUID username; catches IllegalArgumentException
        doNothing().when(employeeService).offboardEmployee(any(), any(), isNull());

        mockMvc.perform(delete("/employees/" + EMP_ID).with(csrf()))
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
