package com.ziyara.backend.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziyara.backend.application.dto.request.CreateWebhookSubscriptionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock JdbcTemplate jdbc;
    @Mock TaskExecutor asyncExecutor;
    @Mock RestTemplate webhookRestTemplate;

    WebhookService service;

    @BeforeEach
    void setUp() {
        service = new WebhookService(jdbc, asyncExecutor, new ObjectMapper(), webhookRestTemplate);
    }

    // ── getSupportedEvents ────────────────────────────────────────────────────

    @Test
    void getSupportedEvents_returnsExpectedList() {
        List<String> events = service.getSupportedEvents();

        assertThat(events).containsExactly("booking.created", "content.approved", "payout.processed");
    }

    // ── create – event validation ─────────────────────────────────────────────

    @Test
    void create_unsupportedEvent_throwsIllegalArgument() {
        CreateWebhookSubscriptionRequest request = new CreateWebhookSubscriptionRequest();
        request.setProviderId(UUID.randomUUID());
        request.setName("My Hook");
        request.setUrl("https://example.com/hook");
        request.setEvents(List.of("booking.created", "nonexistent.event"));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported event: nonexistent.event");
    }

    @Test
    void create_allUnsupportedEvents_throwsIllegalArgument() {
        CreateWebhookSubscriptionRequest request = new CreateWebhookSubscriptionRequest();
        request.setProviderId(UUID.randomUUID());
        request.setName("My Hook");
        request.setUrl("https://example.com/hook");
        request.setEvents(List.of("user.created"));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Supported:")
                .hasMessageContaining("booking.created");
    }
}
