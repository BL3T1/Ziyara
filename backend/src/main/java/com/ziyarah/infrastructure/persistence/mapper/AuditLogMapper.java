package com.ziyarah.infrastructure.persistence.mapper;

import com.ziyarah.domain.entity.AuditLog;
import com.ziyarah.infrastructure.persistence.entity.AuditLogJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper: AuditLogMapper
 */
@Component
public class AuditLogMapper {
    
    public AuditLog toDomainEntity(AuditLogJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        AuditLog auditLog = new AuditLog();
        auditLog.setId(entity.getId());
        auditLog.setAction(entity.getAction());
        auditLog.setEntityName(entity.getEntityName());
        auditLog.setEntityId(entity.getEntityId());
        auditLog.setUserId(entity.getUserId());
        auditLog.setOldValue(entity.getOldValue());
        auditLog.setNewValue(entity.getNewValue());
        auditLog.setIpAddress(entity.getIpAddress());
        auditLog.setUserAgent(entity.getUserAgent());
        auditLog.setCreatedAt(entity.getCreatedAt());
        
        return auditLog;
    }
    
    public AuditLogJpaEntity toJpaEntity(AuditLog auditLog) {
        if (auditLog == null) {
            return null;
        }
        
        return AuditLogJpaEntity.builder()
                .id(auditLog.getId())
                .action(auditLog.getAction())
                .entityName(auditLog.getEntityName())
                .entityId(auditLog.getEntityId())
                .userId(auditLog.getUserId())
                .oldValue(auditLog.getOldValue())
                .newValue(auditLog.getNewValue())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
