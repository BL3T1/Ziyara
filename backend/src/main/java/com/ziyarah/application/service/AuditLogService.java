package com.ziyarah.application.service;

import com.ziyarah.application.dto.response.AuditLogResponse;
import com.ziyarah.domain.entity.AuditLog;
import com.ziyarah.domain.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service: AuditLogService
 * Handles traceability and system activity logging
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {
    
    private final AuditLogRepository auditLogRepository;
    
    @Transactional
    public void logAction(String action, String entityName, String entityId, UUID userId, String oldVal, String newVal, String ip, String ua) {
        log.debug("Logging audit action: {} on {}/{}", action, entityName, entityId);
        
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
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
    
    private AuditLogResponse mapToResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .action(log.getAction())
                .entityName(log.getEntityName())
                .entityId(log.getEntityId())
                .userId(log.getUserId())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
