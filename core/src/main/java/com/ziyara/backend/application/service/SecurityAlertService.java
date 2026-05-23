package com.ziyara.backend.application.service;

import com.ziyara.backend.domain.entity.SecurityAlert;
import com.ziyara.backend.domain.entity.SecurityAlertRule;
import com.ziyara.backend.domain.repository.SecurityAlertRepository;
import com.ziyara.backend.domain.repository.SecurityAlertRuleRepository;
import com.ziyara.backend.domain.repository.SecurityEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Evaluates seeded {@code sys_security_alert_rules} against recent {@code sys_security_events}.
 */
@Service
@RequiredArgsConstructor
public class SecurityAlertService {

    private final SecurityAlertRuleRepository ruleRepository;
    private final SecurityEventRepository eventRepository;
    private final SecurityAlertRepository alertRepository;

    @Transactional
    public void evaluateThresholdAlerts(String ipAddress, String eventType) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }
        List<SecurityAlertRule> rules = ruleRepository.findByEventTypeEnabled(eventType);
        for (SecurityAlertRule rule : rules) {
            Instant since = Instant.now().minus(rule.getTimeWindowMinutes(), ChronoUnit.MINUTES);
            long n = eventRepository.countByTypeAndIpSince(eventType, ipAddress, since);
            if (n == rule.getThreshold().longValue()) {
                SecurityAlert alert = new SecurityAlert();
                alert.setRuleId(rule.getId());
                alert.setTriggeredBy(Map.of(
                        "ip", ipAddress,
                        "windowMinutes", rule.getTimeWindowMinutes(),
                        "count", n,
                        "eventType", eventType));
                alert.setOccurrenceCount((int) n);
                alert.setSeverity(rule.getSeverity());
                alert.setStatus("NEW");
                alertRepository.save(alert);
            }
        }
    }
}
