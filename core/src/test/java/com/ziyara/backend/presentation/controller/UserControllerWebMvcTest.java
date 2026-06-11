package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.command.UserCommandHandler;
import com.ziyara.backend.application.dto.UserResponse;
import com.ziyara.backend.application.query.UserQueryHandler;
import com.ziyara.backend.application.service.CompanyStaffRoleCatalogService;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.application.service.NavigationService;
import com.ziyara.backend.application.service.RbacAssignmentQueryService;
import com.ziyara.backend.application.service.UserRbacAssignmentService;
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
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        UserControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class UserControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockBean UserQueryHandler userQueryHandler;
    @MockBean UserCommandHandler userCommandHandler;
    @MockBean NavigationService navigationService;
    @MockBean UserRbacAssignmentService userRbacAssignmentService;
    @MockBean RbacAssignmentQueryService rbacAssignmentQueryService;
    @MockBean CompanyStaffRoleCatalogService companyStaffRoleCatalogService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    private static final UUID USER_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    // ── GET /users/me ────────────────────────────────────────────────────────

    @Test
    void getMe_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001")
    void getMe_authenticated_returns200() throws Exception {
        UserResponse userResponse = new UserResponse();
        userResponse.setId(USER_ID);
        userResponse.setEmail("user@example.com");
        when(userQueryHandler.findById(USER_ID)).thenReturn(Optional.of(userResponse));

        mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("user@example.com"));
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001")
    void getMe_userNotFound_returns404() throws Exception {
        when(userQueryHandler.findById(USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/me"))
                .andExpect(status().isNotFound());
    }

    // ── GET /users/me/permissions ────────────────────────────────────────────

    @Test
    void getMyPermissions_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/users/me/permissions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001", authorities = {"bookings:read", "users:read"})
    void getMyPermissions_authenticated_returnsCodes() throws Exception {
        mockMvc.perform(get("/users/me/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── PATCH /users/me ──────────────────────────────────────────────────────

    @Test
    void updateMe_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch("/users/me").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Alice\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001")
    void updateMe_authenticated_returns200() throws Exception {
        UserResponse updated = new UserResponse();
        updated.setId(USER_ID);
        updated.setEmail("user@example.com");
        when(userQueryHandler.findById(USER_ID)).thenReturn(Optional.of(updated));

        mockMvc.perform(patch("/users/me").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Alice\"}"))
                .andExpect(status().isOk());
    }

    // ── GET /users ───────────────────────────────���───────────────────────────

    @Test
    void listUsers_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "users:read")
    void listUsers_withPermission_returns200() throws Exception {
        when(userQueryHandler.findPage(any())).thenReturn(Page.empty());

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "bookings:read")
    void listUsers_withoutUsersReadPermission_returns403() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());
    }

    // ── POST /users ───────────────────────────────────────────────────────────

    @Test
    void createUser_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/users").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@example.com\",\"password\":\"Pass123!\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "users:write")
    void createUser_withPermission_returns201() throws Exception {
        UUID newUserId = UUID.randomUUID();
        UserResponse created = new UserResponse();
        created.setId(newUserId);
        created.setEmail("new@example.com");
        when(userCommandHandler.create(any())).thenReturn(newUserId);
        when(userQueryHandler.findById(newUserId)).thenReturn(Optional.of(created));

        mockMvc.perform(post("/users").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@example.com\",\"password\":\"Pass123!\",\"role\":\"CUSTOMER\"}"))
                .andExpect(status().isCreated());
    }

    // ── DELETE /users/{id} ────────────────────────────────────────────────────

    @Test
    void deleteUser_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/users/{id}", USER_ID).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "users:write")
    void deleteUser_withPermission_returns200() throws Exception {
        mockMvc.perform(delete("/users/{id}", USER_ID).with(csrf()))
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
