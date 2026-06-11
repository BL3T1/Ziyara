package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.ReviewResponse;
import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.application.service.ReviewService;
import com.ziyara.backend.domain.enums.ReviewStatus;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReviewController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        ReviewControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class ReviewControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockBean ReviewService reviewService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    private static final UUID USER_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private static final UUID REVIEW_ID = UUID.randomUUID();
    private static final String AUTH_HEADER = "Bearer test-token";

    // ── GET /reviews (admin list) ────────────────────────────────────────────

    @Test
    void listAdmin_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/reviews"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "bookings:read")
    void listAdmin_withCompanyStaffPermission_returns200() throws Exception {
        when(reviewService.listAdmin(anyInt(), anyInt(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/reviews"))
                .andExpect(status().isOk());
    }

    // ── POST /reviews ─────────────────────────────────────────────────────────

    @Test
    void createReview_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/reviews").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":\"00000000-0000-0000-0000-000000000001\",\"rating\":5,\"comment\":\"Great!\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001")
    void createReview_authenticated_returns201() throws Exception {
        ReviewResponse response = ReviewResponse.builder()
                .id(REVIEW_ID)
                .userId(USER_ID)
                .rating(5)
                .comment("Great!")
                .status(ReviewStatus.PENDING)
                .build();
        when(reviewService.createReview(eq(USER_ID), any())).thenReturn(response);

        mockMvc.perform(post("/reviews").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":\"00000000-0000-0000-0000-000000000001\",\"rating\":5,\"comment\":\"Great!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rating").value(5));
    }

    // ── GET /reviews/{id} ─────────────────────────────────────────────────────

    @Test
    void getReview_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/reviews/{id}", REVIEW_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "reviews:read")
    void getReview_withPermission_returns200() throws Exception {
        ReviewResponse response = ReviewResponse.builder().id(REVIEW_ID).build();
        when(reviewService.getReview(REVIEW_ID)).thenReturn(response);

        mockMvc.perform(get("/reviews/{id}", REVIEW_ID))
                .andExpect(status().isOk());
    }

    // ── POST /reviews/{id}/moderate ───────────────────────────────────────────

    @Test
    void moderate_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/reviews/{id}/moderate", REVIEW_ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "reviews:moderate")
    void moderate_withPermission_returns200() throws Exception {
        ReviewResponse response = ReviewResponse.builder().id(REVIEW_ID).status(ReviewStatus.APPROVED).build();
        when(reviewService.moderateReview(eq(REVIEW_ID), any())).thenReturn(response);

        mockMvc.perform(post("/reviews/{id}/moderate", REVIEW_ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "bookings:read")
    void moderate_withoutModeratePermission_returns403() throws Exception {
        mockMvc.perform(post("/reviews/{id}/moderate", REVIEW_ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /reviews/{id} ──────────────────────────────────────────────────

    @Test
    void deleteReview_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/reviews/{id}", REVIEW_ID).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "a0000000-0000-0000-0000-000000000001")
    void deleteReview_authenticated_returns200() throws Exception {
        mockMvc.perform(delete("/reviews/{id}", REVIEW_ID).with(csrf()))
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
