package com.ziyara.backend.application.service;

import com.ziyara.backend.domain.entity.SecurityAlert;
import com.ziyara.backend.domain.entity.SecurityAlertRule;
import com.ziyara.backend.domain.repository.SecurityAlertRepository;
import com.ziyara.backend.domain.repository.SecurityAlertRuleRepository;
import com.ziyara.backend.domain.repository.SecurityEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityAlertServiceTest {

    @Mock SecurityAlertRuleRepository ruleRepository;
    @Mock SecurityEventRepository eventRepository;
    @Mock SecurityAlertRepository alertRepository;

    SecurityAlertService service;

    @BeforeEach
    void setUp() {
        service = new SecurityAlertService(ruleRepository, eventRepository, alertRepository);
    }

    @Test
    void evaluateThresholdAlerts_nullIp_doesNothing() {
        service.evaluateThresholdAlerts(null, "LOGIN_FAILED");
        verifyNoInteractions(ruleRepository, eventRepository, alertRepository);
    }

    @Test
    void evaluateThresholdAlerts_blankIp_doesNothing() {
        service.evaluateThresholdAlerts("   ", "LOGIN_FAILED");
        verifyNoInteractions(ruleRepository, eventRepository, alertRepository);
    }

    @Test
    void evaluateThresholdAlerts_noMatchingRules_doesNotCreateAlert() {
        when(ruleRepository.findByEventTypeEnabled("LOGIN_FAILED")).thenReturn(List.of());

        service.evaluateThresholdAlerts("192.168.1.1", "LOGIN_FAILED");

        verifyNoInteractions(eventRepository, alertRepository);
    }

    @Test
    void evaluateThresholdAlerts_countBelowThreshold_doesNotCreateAlert() {
        SecurityAlertRule rule = ruleWithThreshold(5, 10, "HIGH");
        when(ruleRepository.findByEventTypeEnabled("LOGIN_FAILED")).thenReturn(List.of(rule));
        when(eventRepository.countByTypeAndIpSince(anyString(), anyString(), any(Instant.class))).thenReturn(3L);

        service.evaluateThresholdAlerts("192.168.1.1", "LOGIN_FAILED");

        verifyNoInteractions(alertRepository);
    }

    @Test
    void evaluateThresholdAlerts_countExactlyAtThreshold_createsAlert() {
        SecurityAlertRule rule = ruleWithThreshold(5, 10, "HIGH");
        when(ruleRepository.findByEventTypeEnabled("LOGIN_FAILED")).thenReturn(List.of(rule));
        when(eventRepository.countByTypeAndIpSince(anyString(), anyString(), any(Instant.class))).thenReturn(5L);

        service.evaluateThresholdAlerts("192.168.1.1", "LOGIN_FAILED");

        ArgumentCaptor<SecurityAlert> captor = ArgumentCaptor.forClass(SecurityAlert.class);
        verify(alertRepository).save(captor.capture());
        SecurityAlert saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("NEW");
        assertThat(saved.getSeverity()).isEqualTo("HIGH");
        assertThat(saved.getOccurrenceCount()).isEqualTo(5);
        assertThat(saved.getTriggeredBy()).containsEntry("ip", "192.168.1.1");
    }

    @Test
    void evaluateThresholdAlerts_countAboveThreshold_doesNotCreateAlert() {
        SecurityAlertRule rule = ruleWithThreshold(5, 10, "HIGH");
        when(ruleRepository.findByEventTypeEnabled("LOGIN_FAILED")).thenReturn(List.of(rule));
        when(eventRepository.countByTypeAndIpSince(anyString(), anyString(), any(Instant.class))).thenReturn(6L);

        service.evaluateThresholdAlerts("192.168.1.1", "LOGIN_FAILED");

        verifyNoInteractions(alertRepository);
    }

    @Test
    void evaluateThresholdAlerts_multipleRules_createsAlertForMatchingOnly() {
        SecurityAlertRule ruleA = ruleWithThreshold(3, 5, "MEDIUM");
        SecurityAlertRule ruleB = ruleWithThreshold(10, 60, "CRITICAL");
        when(ruleRepository.findByEventTypeEnabled("LOGIN_FAILED")).thenReturn(List.of(ruleA, ruleB));
        when(eventRepository.countByTypeAndIpSince(anyString(), anyString(), any(Instant.class))).thenReturn(3L);

        service.evaluateThresholdAlerts("10.0.0.1", "LOGIN_FAILED");

        verify(alertRepository, times(1)).save(any());
    }

    private SecurityAlertRule ruleWithThreshold(int threshold, int windowMinutes, String severity) {
        SecurityAlertRule rule = new SecurityAlertRule();
        rule.setId(UUID.randomUUID());
        rule.setEventType("LOGIN_FAILED");
        rule.setThreshold(threshold);
        rule.setTimeWindowMinutes(windowMinutes);
        rule.setSeverity(severity);
        rule.setEnabled(true);
        return rule;
    }
}
