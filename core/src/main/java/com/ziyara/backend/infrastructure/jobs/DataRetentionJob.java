package com.ziyara.backend.infrastructure.jobs;

import com.ziyara.backend.infrastructure.persistence.entity.DataRetentionPolicyJpaEntity;
import com.ziyara.backend.infrastructure.persistence.repository.DataRetentionPolicyJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Executes enabled rows in {@code sys_data_retention_policies} (OTP cleanup, password reset tokens, audit archive batches).
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "ziyara.data-retention", name = "enabled", havingValue = "true")
public class DataRetentionJob {

    private final DataRetentionPolicyJpaRepository policyRepository;
    private final JdbcTemplate jdbcTemplate;

    @Scheduled(cron = "${ziyara.data-retention.cron:0 0 4 * * SUN}")
    @Transactional
    public void runWeekly() {
        for (DataRetentionPolicyJpaEntity p : policyRepository.findByEnabledTrue()) {
            try {
                apply(p);
            } catch (Exception e) {
                log.warn("Retention policy {} failed: {}", p.getEntityType(), e.getMessage());
            }
        }
    }

    private void apply(DataRetentionPolicyJpaEntity p) {
        Instant cutoff = Instant.now().minus(Duration.ofDays(p.getRetentionPeriodDays()));
        Timestamp ts = Timestamp.from(cutoff);
        int affected = switch (p.getEntityType()) {
            case "sys_otp_verification" -> jdbcTemplate.update(
                    "DELETE FROM sys_otp_verification WHERE expires_at < ?", ts);
            case "sys_password_reset_tokens" -> jdbcTemplate.update(
                    "DELETE FROM sys_password_reset_tokens WHERE expires_at < ?", ts);
            case "sys_audit_logs" -> archiveAuditLogs(ts);
            default -> 0;
        };
        p.setLastExecution(LocalDateTime.now());
        p.setNextExecution(LocalDateTime.now().plusDays(Math.max(1, p.getRetentionPeriodDays() / 2)));
        policyRepository.save(p);
        log.info("Retention {} ({}) affected ~{} rows", p.getEntityType(), p.getAction(), affected);
    }

    private int archiveAuditLogs(Timestamp cutoff) {
        List<UUID> ids = jdbcTemplate.query(
                "SELECT id FROM sys_audit_logs WHERE created_at < ? ORDER BY created_at ASC LIMIT 500",
                (rs, row) -> rs.getObject("id", UUID.class),
                cutoff);
        if (ids.isEmpty()) {
            return 0;
        }
        String in = String.join(",", Collections.nCopies(ids.size(), "?"));
        Object[] idArgs = ids.toArray();
        jdbcTemplate.update(
                """
                        INSERT INTO sys_audit_logs_archive (id, action, entity_type, entity_name, entity_id, user_id, old_value, new_value, ip_address, user_agent, created_at, correlation_id, request_id, session_id, provider_id, tenant_id, risk_score, duration_ms, tags)
                        SELECT id, action, entity_type, entity_name, entity_id, user_id, old_value, new_value, ip_address, user_agent, created_at, correlation_id, request_id, session_id, provider_id, tenant_id, risk_score, duration_ms, tags
                        FROM sys_audit_logs WHERE id IN ("""
                        + in + ") ON CONFLICT (id) DO NOTHING",
                idArgs);
        return jdbcTemplate.update("DELETE FROM sys_audit_logs WHERE id IN (" + in + ")", idArgs);
    }
}
