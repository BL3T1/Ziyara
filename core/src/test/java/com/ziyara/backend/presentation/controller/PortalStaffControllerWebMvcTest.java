package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.request.AddPortalStaffRequest;
import com.ziyara.backend.application.dto.request.CreatePortalStaffUserRequest;
import com.ziyara.backend.application.dto.response.PortalStaffMemberResponse;
import com.ziyara.backend.application.dto.response.ServiceProviderResponse;
import com.ziyara.backend.application.service.PortalStaffService;
import com.ziyara.backend.application.service.ServiceProviderService;
import com.ziyara.backend.domain.enums.UserRole;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PortalStaffController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        PortalStaffControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class PortalStaffControllerWebMvcTest {

    private static final UUID PORTAL_USER_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID PROVIDER_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private static final UUID STAFF_USER_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PortalStaffService portalStaffService;

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
                        .name("Test Hotel")
                        .build()));
    }

    @Test
    @WithMockUser(username = "10000000-0000-4000-8000-000000000001", roles = "PROVIDER_MANAGER")
    void listStaff_returns200() throws Exception {
        stubCurrentProvider();
        PortalStaffMemberResponse row = PortalStaffMemberResponse.builder()
                .userId(STAFF_USER_ID)
                .email("staff@example.com")
                .role(UserRole.PROVIDER_STAFF)
                .owner(false)
                .build();
        when(portalStaffService.listStaff(PROVIDER_ID)).thenReturn(List.of(row));

        mockMvc.perform(get("/portal/staff").header("Authorization", "Bearer test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].email").value("staff@example.com"))
                .andExpect(jsonPath("$.data[0].owner").value(false));

        verify(portalStaffService).listStaff(PROVIDER_ID);
    }

    @Test
    @WithMockUser(username = "10000000-0000-4000-8000-000000000001", roles = "PROVIDER_MANAGER")
    void addStaff_returns201() throws Exception {
        stubCurrentProvider();
        PortalStaffMemberResponse created = PortalStaffMemberResponse.builder()
                .staffLinkId(UUID.fromString("40000000-0000-4000-8000-000000000001"))
                .userId(STAFF_USER_ID)
                .email("new@example.com")
                .role(UserRole.PROVIDER_STAFF)
                .title("Ops")
                .owner(false)
                .build();
        when(portalStaffService.addStaff(eq(PROVIDER_ID), any(AddPortalStaffRequest.class))).thenReturn(created);

        mockMvc.perform(post("/portal/staff")
                        .header("Authorization", "Bearer test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"30000000-0000-4000-8000-000000000001\",\"title\":\"Ops\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Ops"));
    }

    @Test
    @WithMockUser(username = "10000000-0000-4000-8000-000000000001", roles = "PROVIDER_MANAGER")
    void createStaffUser_returns201() throws Exception {
        stubCurrentProvider();
        PortalStaffMemberResponse created = PortalStaffMemberResponse.builder()
                .staffLinkId(UUID.fromString("50000000-0000-4000-8000-000000000001"))
                .userId(STAFF_USER_ID)
                .email("new.staff@example.com")
                .role(UserRole.PROVIDER_STAFF)
                .title("Desk")
                .owner(false)
                .build();
        when(portalStaffService.createStaffUser(eq(PROVIDER_ID), eq(PORTAL_USER_ID), eq(UserRole.PROVIDER_MANAGER), any(CreatePortalStaffUserRequest.class)))
                .thenReturn(created);

        mockMvc.perform(post("/portal/staff/users")
                        .header("Authorization", "Bearer test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new.staff@example.com\",\"password\":\"secret12\",\"role\":\"PROVIDER_STAFF\",\"title\":\"Desk\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("new.staff@example.com"));
    }

    @Test
    @WithMockUser(username = "10000000-0000-4000-8000-000000000001", roles = "PROVIDER_MANAGER")
    void updateStaff_returns200() throws Exception {
        stubCurrentProvider();
        PortalStaffMemberResponse updated = PortalStaffMemberResponse.builder()
                .userId(STAFF_USER_ID)
                .title("Lead")
                .owner(false)
                .build();
        when(portalStaffService.updateStaff(eq(PROVIDER_ID), eq(STAFF_USER_ID), any())).thenReturn(updated);

        mockMvc.perform(put("/portal/staff/{userId}", STAFF_USER_ID)
                        .header("Authorization", "Bearer test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Lead\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Lead"));
    }

    @Test
    @WithMockUser(username = "10000000-0000-4000-8000-000000000001", roles = "PROVIDER_MANAGER")
    void removeStaff_returns200() throws Exception {
        stubCurrentProvider();

        mockMvc.perform(delete("/portal/staff/{userId}", STAFF_USER_ID)
                        .header("Authorization", "Bearer test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(portalStaffService).removeStaff(PROVIDER_ID, STAFF_USER_ID);
    }

    @Test
    @WithMockUser(username = "10000000-0000-4000-8000-000000000001", roles = "CUSTOMER")
    void listStaff_forbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/portal/staff").header("Authorization", "Bearer test"))
                .andExpect(status().isForbidden());
    }
}
