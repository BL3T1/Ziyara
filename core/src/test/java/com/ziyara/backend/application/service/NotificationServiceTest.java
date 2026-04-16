package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateNotificationRequest;
import com.ziyara.backend.application.dto.response.NotificationResponse;
import com.ziyara.backend.domain.entity.Notification;
import com.ziyara.backend.domain.enums.NotificationType;
import com.ziyara.backend.domain.enums.NotificationChannel;
import com.ziyara.backend.domain.repository.NotificationRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.NotificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationService notificationService;

    private CreateNotificationRequest request;
    private Notification notification;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        request = CreateNotificationRequest.builder()
                .userId(userId)
                .type(NotificationType.SYSTEM_ALERT)
                .channel(NotificationChannel.EMAIL)
                .title("Test")
                .message("Hello")
                .build();

        notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setUserId(userId);
    }

    @Test
    void createNotification_ShouldSaveAndReturnResponse() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationResponse response = notificationService.createNotification(request);

        assertNotNull(response);
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }
}
