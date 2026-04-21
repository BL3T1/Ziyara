package com.ziyara.backend.application.service;

import com.ziyara.backend.infrastructure.persistence.entity.SecurityAlertJpaEntity;
import com.ziyara.backend.infrastructure.persistence.entity.SecurityAlertRuleJpaEntity;
import com.ziyara.backend.infrastructure.persistence.repository.SecurityAlertJpaRepository;
import com.ziyara.backend.infrastructure.persistence.repository.SecurityAlertRuleJpaRepository;
import com.ziyara.backend.infrastructure.persistence.repository.SecurityEventJpaRepository;
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

    private final SecurityAlertRuleJpaRepository ruleJpaRepository;
    private final SecurityEventJpaRepository eventJpaRepository;
    private final SecurityAlertJpaRepository alertJpaRepository;

    @Transactional
    public void evaluateThresholdAlerts(String ipAddress, String eventType) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }
        List<SecurityAlertRuleJpaEntity> rules = ruleJpaRepository.findByEventTypeAndEnabledTrue(eventType);
        for (SecurityAlertRuleJpaEntity rule : rules) {
            Instant since = Instant.now().minus(rule.getTimeWindowMinutes(), ChronoUnit.MINUTES);
            long n = eventJpaRepository.countByTypeAndIpSince(eventType, ipAddress, since);
            if (n == rule.getThreshold().longValue()) {
                SecurityAlertJpaEntity alert = SecurityAlertJpaEntity.builder()
                        .ruleId(rule.getId())
                        .triggeredBy(Map.of("ip", ipAddress, "windowMinutes", rule.getTimeWindowMinutes(), "count", n, "eventType", eventType))
                        .occurrenceCount((int) n)
                        .severity(rule.getSeverity())
                        .status("NEW")
                        .build();
                alertJpaRepository.save(alert);
            }
        }
    }
}
