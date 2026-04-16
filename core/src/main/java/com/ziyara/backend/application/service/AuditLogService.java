package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.AuditLogResponse;
import com.ziyara.backend.domain.entity.AuditLog;
import com.ziyara.backend.modules.sys.api.AuditServiceApi;
import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.repository.AuditLogRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Pageable pageable = PageRequest.of(0, Math.min(limit, 100));
        List<AuditLog> logs = auditLogRepository.findRecent(pageable).getContent();
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
                .entityId(log.getEntityId())
                .userId(log.getUserId())
                .createdAt(log.getCreatedAt())
                .userDisplay(userDisplay)
                .resource(resource.isEmpty() ? "-" : resource)
                .status("Success")
                .build();
    }
}
