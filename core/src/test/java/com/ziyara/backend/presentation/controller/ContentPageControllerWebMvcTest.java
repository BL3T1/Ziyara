package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.ContentPageResponse;
import com.ziyara.backend.application.service.ContentPageService;
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
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ContentPageController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        ContentPageControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class ContentPageControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockBean ContentPageService contentPageService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    // ── GET /content-pages/{slug} — public endpoint ───────────────────────────

    @Test
    void getPublicPage_noAuth_returns200() throws Exception {
        ContentPageResponse response = ContentPageResponse.builder().slug("about").build();
        when(contentPageService.getPublicPage(eq("about"), any())).thenReturn(response);

        mockMvc.perform(get("/content-pages/about"))
                .andExpect(status().isOk());
    }

    @Test
    void getPublicPage_withLangParam_returns200() throws Exception {
        ContentPageResponse response = ContentPageResponse.builder().slug("privacy").build();
        when(contentPageService.getPublicPage(eq("privacy"), eq("ar"))).thenReturn(response);

        mockMvc.perform(get("/content-pages/privacy").param("lang", "ar"))
                .andExpect(status().isOk());
    }

    // ── PUT /content-pages/{slug} — requires content:write ───────────────────

    @Test
    void upsertPage_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/content-pages/about")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titleEn\":\"About Us\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "content:write")
    void upsertPage_withContentWrite_returns200() throws Exception {
        ContentPageResponse response = ContentPageResponse.builder().slug("about").build();
        when(contentPageService.upsert(eq("about"), any())).thenReturn(response);

        mockMvc.perform(put("/content-pages/about")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titleEn\":\"About Us\",\"bodyEn\":\"Body text\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "bookings:read")
    void upsertPage_withoutContentWrite_returns403() throws Exception {
        mockMvc.perform(put("/content-pages/about")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titleEn\":\"About Us\"}"))
                .andExpect(status().isForbidden());
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
