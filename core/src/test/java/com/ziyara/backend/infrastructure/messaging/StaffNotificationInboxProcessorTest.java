package com.ziyara.backend.infrastructure.messaging;

import com.ziyara.backend.application.dto.request.CreateNotificationRequest;
import com.ziyara.backend.application.service.NotificationService;
import com.ziyara.backend.domain.enums.NotificationChannel;
import com.ziyara.backend.domain.enums.NotificationType;
import com.ziyara.backend.infrastructure.persistence.entity.KafkaStaffNotificationDeliveredEntity;
import com.ziyara.backend.infrastructure.persistence.repository.KafkaStaffNotificationDeliveredJpaRepository;
import com.ziyara.backend.application.dto.StaffNotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffNotificationInboxProcessorTest {

    @Mock
    private StaffNotificationRecipientResolver recipientResolver;

    @Mock
    private NotificationService notificationService;

    @Mock
    private KafkaStaffNotificationDeliveredJpaRepository deliveredRepository;

    private StaffNotificationInboxProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new StaffNotificationInboxProcessor(recipientResolver, notificationService, deliveredRepository);
    }

    @Test
    void process_directRecipient_createsNotificationAndDelivery() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(deliveredRepository.existsDelivery(eventId, userId)).thenReturn(false);

        StaffNotificationEvent event = StaffNotificationEvent.builder()
                .eventId(eventId)
                .notificationType(NotificationType.SYSTEM_ALERT.name())
                .title("T")
                .message("M")
                .recipientUserId(userId)
                .build();

        processor.process(event);

        ArgumentCaptor<CreateNotificationRequest> cap = ArgumentCaptor.forClass(CreateNotificationRequest.class);
        verify(notificationService).createNotification(cap.capture());
        assertEquals(userId, cap.getValue().getUserId());
        assertEquals(NotificationChannel.IN_APP, cap.getValue().getChannel());
        verify(deliveredRepository).save(any(KafkaStaffNotificationDeliveredEntity.class));
    }

    @Test
    void process_skipsWhenAlreadyDelivered() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(deliveredRepository.existsDelivery(eventId, userId)).thenReturn(true);

        processor.process(StaffNotificationEvent.builder()
                .eventId(eventId)
                .notificationType("UNKNOWN_TYPE_XYZ")
                .title("T")
                .message("M")
                .recipientUserId(userId)
                .build());

        verify(notificationService, never()).createNotification(any());
    }

    @Test
    void process_rolesResolved_mergesRecipients() {
        UUID eventId = UUID.randomUUID();
        UUID u1 = UUID.randomUUID();
        when(recipientResolver.resolveRoleNames(List.of("SUPPORT_MANAGER"))).thenReturn(List.of(u1));
        when(deliveredRepository.existsDelivery(eventId, u1)).thenReturn(false);

        processor.process(StaffNotificationEvent.builder()
                .eventId(eventId)
                .notificationType(NotificationType.COMPLAINT_NEW.name())
                .title("New")
                .message("Body")
                .notifyRoles(List.of("SUPPORT_MANAGER"))
                .build());

        verify(notificationService).createNotification(any());
        verify(deliveredRepository).save(any());
    }
}
