package com.ziyarah.infrastructure.persistence.adapter;

import com.ziyarah.domain.entity.AuditLog;
import com.ziyarah.domain.repository.AuditLogRepository;
import com.ziyarah.infrastructure.persistence.entity.AuditLogJpaEntity;
import com.ziyarah.infrastructure.persistence.mapper.AuditLogMapper;
import com.ziyarah.infrastructure.persistence.repository.AuditLogJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository Adapter: AuditLogRepositoryAdapter
 */
@Component
@RequiredArgsConstructor
public class AuditLogRepositoryAdapter implements AuditLogRepository {
    
    private final AuditLogJpaRepository auditLogJpaRepository;
    private final AuditLogMapper auditLogMapper;
    
    @Override
    public AuditLog save(AuditLog auditLog) {
        AuditLogJpaEntity entity = auditLogMapper.toJpaEntity(auditLog);
        AuditLogJpaEntity savedEntity = auditLogJpaRepository.save(entity);
        return auditLogMapper.toDomainEntity(savedEntity);
    }
    
    @Override
    public Optional<AuditLog> findById(UUID id) {
        return auditLogJpaRepository.findById(id)
                .map(auditLogMapper::toDomainEntity);
    }
    
    @Override
    public List<AuditLog> findByEntityNameAndEntityId(String entityName, String entityId) {
        return auditLogJpaRepository.findByEntityNameAndEntityId(entityName, entityId).stream()
                .map(auditLogMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<AuditLog> findByUserId(UUID userId) {
        return auditLogJpaRepository.findByUserId(userId).stream()
                .map(auditLogMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
}
