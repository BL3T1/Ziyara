package com.ziyarah.presentation.controller;

import com.ziyarah.application.dto.response.NotificationResponse;
import com.ziyarah.application.service.NotificationService;
import com.ziyarah.infrastructure.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
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
        when(notificationService.getUserNotifications(userId)).thenReturn(List.of(
                NotificationResponse.builder().id(UUID.randomUUID()).title("Hello").build()
        ));

        mockMvc.perform(get("/api/v1/notifications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Hello"));
    }
}
