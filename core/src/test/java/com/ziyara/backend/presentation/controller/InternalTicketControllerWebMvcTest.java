package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.TicketRequest;
import com.ziyara.backend.application.dto.TicketResponse;
import com.ziyara.backend.application.service.InternalTicketService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = InternalTicketController.class)
@Import({
        SecurityConfig.class,
        WebMvcConfigurationPropertiesImport.class,
        WebMvcSecuritySliceConfiguration.class,
        LocaleConfig.class,
        InternalTicketControllerWebMvcTest.SecurityBeans.class
})
@ActiveProfiles("test")
class InternalTicketControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockBean InternalTicketService ticketService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;

    private static final UUID TICKET_ID = UUID.randomUUID();

    // ── POST /tickets ─────────────────────────────────────────────────────────

    @Test
    void createTicket_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/tickets").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bug\",\"type\":\"BUG\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", authorities = "tickets:write")
    void createTicket_withCompanyStaff_returns201() throws Exception {
        TicketResponse response = new TicketResponse();
        response.setId(TICKET_ID);
        when(ticketService.createTicket(any(), any())).thenReturn(response);

        mockMvc.perform(post("/tickets").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bug\",\"type\":\"BUG\",\"priority\":\"HIGH\",\"description\":\"Desc\"}"))
                .andExpect(status().isCreated());
    }

    // ── GET /tickets ──────────────────────────────────────────────────────────

    @Test
    void listTickets_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/tickets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "tickets:read")
    void listTickets_withCompanyStaff_returns200() throws Exception {
        when(ticketService.listTickets(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/tickets"))
                .andExpect(status().isOk());
    }

    // ── GET /tickets/{id} ─────────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "tickets:read")
    void getTicket_found_returns200() throws Exception {
        TicketResponse response = new TicketResponse();
        response.setId(TICKET_ID);
        when(ticketService.getTicket(TICKET_ID.toString())).thenReturn(response);

        mockMvc.perform(get("/tickets/{id}", TICKET_ID))
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
