package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.response.NotificationInboxResponse;
import com.ziyara.backend.application.dto.response.NotificationResponse;
import com.ziyara.backend.application.service.NotificationService;
import com.ziyara.backend.infrastructure.security.JwtService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Disabled("Covered by OpenAPI endpoint smoke test; enable after wiring full controller slice")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private JwtService jwtService;

    @Test
    void getMyNotifications_ShouldReturnList() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = "dummy-token";
        
        when(jwtService.extractUserId(any())).thenReturn(userId.toString());
        var page = new PageImpl<>(
                List.of(NotificationResponse.builder().id(UUID.randomUUID()).title("Hello").build()),
                PageRequest.of(0, 20),
                1);
        when(notificationService.getUserNotificationsInbox(eq(userId), eq(0), eq(20)))
                .thenReturn(new NotificationInboxResponse(page, 1L));

        mockMvc.perform(get("/api/v1/notifications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.notifications.content[0].title").value("Hello"));
    }
}
