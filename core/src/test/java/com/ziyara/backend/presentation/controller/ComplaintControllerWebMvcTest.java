package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.ComplaintCommentResponse;
import com.ziyara.backend.application.dto.response.ComplaintResponse;
import com.ziyara.backend.application.query.ComplaintQueryHandler;
import com.ziyara.backend.application.service.ComplaintCommentService;
import com.ziyara.backend.application.service.ComplaintService;
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
import org.springframework.data.domain.Page;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ComplaintController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        ComplaintControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class ComplaintControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockBean ComplaintService complaintService;
    @MockBean ComplaintCommentService commentService;
    @MockBean ComplaintQueryHandler complaintQueryHandler;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID COMPLAINT_ID = UUID.randomUUID();

    // ── GET /complaints ───────────────────────────────────────────────────────

    @Test
    void listComplaints_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/complaints"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "complaints:read")
    void listComplaints_withCompanyStaff_returns200() throws Exception {
        when(complaintQueryHandler.findPage(anyInt(), anyInt(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/complaints"))
                .andExpect(status().isOk());
    }

    // ── GET /complaints/{id} ──────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "complaints:read")
    void getById_found_returns200() throws Exception {
        ComplaintResponse response = ComplaintResponse.builder().id(COMPLAINT_ID).build();
        when(complaintQueryHandler.findById(COMPLAINT_ID)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/complaints/{id}", COMPLAINT_ID))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "complaints:read")
    void getById_notFound_returns404() throws Exception {
        when(complaintQueryHandler.findById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/complaints/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ── POST /complaints ──────────────────────────────────────────────────────

    @Test
    void createComplaint_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Test\",\"description\":\"Desc\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "complaints:write")
    void createComplaint_withCompanyStaff_returns201() throws Exception {
        when(jwtService.extractUserId("test-token")).thenReturn(USER_ID.toString());
        ComplaintResponse response = ComplaintResponse.builder().id(COMPLAINT_ID).build();
        when(complaintService.create(any(), eq(USER_ID))).thenReturn(response);

        mockMvc.perform(post("/complaints")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Test\",\"description\":\"Desc\"}"))
                .andExpect(status().isCreated());
    }

    // ── PATCH /complaints/{id} ────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "complaints:write")
    void updateComplaint_withCompanyStaff_returns200() throws Exception {
        ComplaintResponse response = ComplaintResponse.builder().id(COMPLAINT_ID).build();
        when(complaintService.update(eq(COMPLAINT_ID), any())).thenReturn(response);

        mockMvc.perform(patch("/complaints/{id}", COMPLAINT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Updated\"}"))
                .andExpect(status().isOk());
    }

    // ── POST /complaints/{id}/assign ──────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "complaints:write")
    void assignComplaint_withCompanyStaff_returns200() throws Exception {
        ComplaintResponse response = ComplaintResponse.builder().id(COMPLAINT_ID).build();
        when(complaintService.assign(eq(COMPLAINT_ID), any())).thenReturn(response);

        mockMvc.perform(post("/complaints/{id}/assign", COMPLAINT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedTo\":\"" + USER_ID + "\"}"))
                .andExpect(status().isOk());
    }

    // ── POST /complaints/{id}/resolve ─────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "complaints:write")
    void resolveComplaint_withCompanyStaff_returns200() throws Exception {
        when(jwtService.extractUserId("test-token")).thenReturn(USER_ID.toString());
        ComplaintResponse response = ComplaintResponse.builder().id(COMPLAINT_ID).build();
        when(complaintService.resolve(eq(COMPLAINT_ID), any(), eq(USER_ID))).thenReturn(response);

        mockMvc.perform(post("/complaints/{id}/resolve", COMPLAINT_ID)
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    // ── POST /complaints/{id}/close ───────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "complaints:write")
    void closeComplaint_withCompanyStaff_returns200() throws Exception {
        ComplaintResponse response = ComplaintResponse.builder().id(COMPLAINT_ID).build();
        when(complaintService.close(COMPLAINT_ID)).thenReturn(response);

        mockMvc.perform(post("/complaints/{id}/close", COMPLAINT_ID))
                .andExpect(status().isOk());
    }

    // ── GET /complaints/{id}/comments ─────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "complaints:read")
    void getComments_withCompanyStaff_returns200() throws Exception {
        when(commentService.getComplaintComments(eq(COMPLAINT_ID), anyBoolean()))
                .thenReturn(List.of());

        mockMvc.perform(get("/complaints/{id}/comments", COMPLAINT_ID))
                .andExpect(status().isOk());
    }

    // ── POST /complaints/{id}/comments ────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "complaints:write")
    void addComment_withCompanyStaff_returns200() throws Exception {
        when(jwtService.extractUserId("test-token")).thenReturn(USER_ID.toString());
        when(commentService.addComment(eq(COMPLAINT_ID), eq(USER_ID), any()))
                .thenReturn(new ComplaintCommentResponse());

        mockMvc.perform(post("/complaints/{id}/comments", COMPLAINT_ID)
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Comment text\"}"))
                .andExpect(status().isOk());
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
