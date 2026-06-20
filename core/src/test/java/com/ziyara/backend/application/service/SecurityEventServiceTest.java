package com.ziyara.backend.application.service;

import com.ziyara.backend.domain.entity.SecurityEvent;
import com.ziyara.backend.domain.repository.SecurityEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SecurityEventServiceTest {

    @Mock SecurityEventRepository securityEventRepository;

    SecurityEventService service;

    @BeforeEach
    void setUp() {
        service = new SecurityEventService(securityEventRepository);
    }

    @Test
    void record_withAllFields_savesEvent() {
        UUID userId = UUID.randomUUID();
        Map<String, Object> details = Map.of("reason", "invalid_password");

        service.record(userId, "LOGIN_FAILED", "MEDIUM", "10.0.0.1", "Mozilla/5.0", details);

        ArgumentCaptor<SecurityEvent> captor = ArgumentCaptor.forClass(SecurityEvent.class);
        verify(securityEventRepository).save(captor.capture());
        SecurityEvent saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getEventType()).isEqualTo("LOGIN_FAILED");
        assertThat(saved.getSeverity()).isEqualTo("MEDIUM");
        assertThat(saved.getIpAddress()).isEqualTo("10.0.0.1");
        assertThat(saved.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(saved.getDetails()).containsEntry("reason", "invalid_password");
    }

    @Test
    void record_withNullUserId_savesEventWithNullUser() {
        service.record(null, "ANONYMOUS_ACCESS", "LOW", "1.2.3.4", null, null);

        ArgumentCaptor<SecurityEvent> captor = ArgumentCaptor.forClass(SecurityEvent.class);
        verify(securityEventRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isNull();
    }

    @Test
    void record_withNullDetails_savesEventWithNullDetails() {
        service.record(UUID.randomUUID(), "LOGIN_SUCCESS", "INFO", "10.0.0.1", "curl/7.0", null);

        ArgumentCaptor<SecurityEvent> captor = ArgumentCaptor.forClass(SecurityEvent.class);
        verify(securityEventRepository).save(captor.capture());
        assertThat(captor.getValue().getDetails()).isNull();
    }

    @Test
    void record_withEmptyDetails_savesEventWithNullDetails() {
        service.record(UUID.randomUUID(), "LOGIN_SUCCESS", "INFO", "10.0.0.1", null, Map.of());

        ArgumentCaptor<SecurityEvent> captor = ArgumentCaptor.forClass(SecurityEvent.class);
        verify(securityEventRepository).save(captor.capture());
        assertThat(captor.getValue().getDetails()).isNull();
    }

    @Test
    void record_doesNotMutateOriginalDetailsMap() {
        Map<String, Object> original = new java.util.HashMap<>();
        original.put("k", "v");

        service.record(UUID.randomUUID(), "TEST", "LOW", "1.2.3.4", null, original);

        assertThat(original).containsKey("k");
    }
}
