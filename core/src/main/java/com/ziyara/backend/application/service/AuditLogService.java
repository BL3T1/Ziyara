package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.AuditLogResponse;
import com.ziyara.backend.domain.entity.AuditLog;
import com.ziyara.backend.modules.sys.api.AuditServiceApi;
import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.repository.AuditLogRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.infrastructure.web.AuditRequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ziyara.backend.domain.common.PageQuery;
import com.ziyara.backend.infrastructure.persistence.util.PageConverter;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service: AuditLogService – implements AuditServiceApi (Phase 3).
 * Handles traceability and system activity logging.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService implements AuditServiceApi {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public void logAction(String action, String entityName, String entityId, UUID userId, String oldVal, String newVal, String ip, String ua) {
        log.debug("Logging audit action: {} on {}/{}", action, entityName, entityId);
        
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        // DB schema requires entity_type (NOT NULL). Keep legacy entityName for readers/search.
        auditLog.setEntityType(entityName != null && !entityName.isBlank() ? entityName : "System");
        auditLog.setEntityName(entityName);
        auditLog.setEntityId(entityId);
        auditLog.setUserId(userId);
        auditLog.setOldValue(oldVal);
        auditLog.setNewValue(newVal);
        auditLog.setIpAddress(ip);
        auditLog.setUserAgent(ua);
        AuditRequestContext.Holder ctx = AuditRequestContext.get();
        if (ctx != null) {
            auditLog.setCorrelationId(ctx.correlationId());
            auditLog.setRequestId(ctx.requestId());
            auditLog.setSessionId(ctx.sessionId());
            auditLog.setProviderId(ctx.providerId());
            auditLog.setTenantId(ctx.tenantId());
            auditLog.setRiskScore(ctx.riskScore());
            auditLog.setDurationMs(ctx.durationMs());
            auditLog.setTags(ctx.tags());
        }

        auditLogRepository.save(auditLog);
    }
    
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getEntityLogs(String entityName, String entityId) {
        return auditLogRepository.findByEntityNameAndEntityId(entityName, entityId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getUserLogs(UUID userId) {
        return auditLogRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getRecentLogs(int limit, String search) {
        PageQuery query = PageQuery.of(0, Math.min(limit, 100));
        List<AuditLog> logs = auditLogRepository.findRecent(query).content();
        if (search != null && !search.isBlank()) {
            String term = search.trim().toLowerCase();
            logs = logs.stream()
                    .filter(l -> (l.getAction() != null && l.getAction().toLowerCase().contains(term))
                            || (l.getEntityName() != null && l.getEntityName().toLowerCase().contains(term))
                            || (l.getEntityId() != null && l.getEntityId().toLowerCase().contains(term)))
                    .collect(Collectors.toList());
        }
        return logs.stream().map(this::mapToResponseWithDisplay).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public Page<AuditLogResponse> getFilteredLogs(String entityType,
                                                   String action,
                                                   UUID userId,
                                                   LocalDateTime dateFrom,
                                                   LocalDateTime dateTo,
                                                   int page,
                                                   int size) {
        PageQuery query = PageQuery.of(page, Math.min(size, 200));
        // Substitute open-ended bounds for null timestamps to avoid PostgreSQL
        // parameter type-inference failure ("could not determine data type of parameter $N").
        LocalDateTime from = dateFrom != null ? dateFrom : LocalDateTime.of(1900, 1, 1, 0, 0);
        LocalDateTime to   = dateTo   != null ? dateTo   : LocalDateTime.of(9999, 12, 31, 23, 59);
        return PageConverter.toSpringPage(
                auditLogRepository.findFiltered(entityType, action, userId, from, to, query),
                query, this::mapToResponseWithDisplay);
    }

    private AuditLogResponse mapToResponse(AuditLog log) {
        return mapToResponseWithDisplay(log);
    }

    private AuditLogResponse mapToResponseWithDisplay(AuditLog log) {
        String userDisplay = "system";
        if (log.getUserId() != null) {
            userDisplay = userRepository.findById(log.getUserId()).map(User::getEmail).orElse("system");
        }
        String resource = (log.getEntityName() != null ? log.getEntityName() : "") + (log.getEntityId() != null ? "/" + log.getEntityId() : "");
        return AuditLogResponse.builder()
                .id(log.getId())
                .action(log.getAction())
                .entityName(log.getEntityName())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .userId(log.getUserId())
                .correlationId(log.getCorrelationId())
                .createdAt(log.getCreatedAt())
                .userDisplay(userDisplay)
                .resource(resource.isEmpty() ? "-" : resource)
                .status("Success")
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .build();
    }
}
