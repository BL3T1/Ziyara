package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.common.PageQuery;
import com.ziyara.backend.domain.common.PagedResult;
import com.ziyara.backend.domain.entity.AuditLog;
import com.ziyara.backend.domain.repository.AuditLogRepository;
import com.ziyara.backend.infrastructure.persistence.entity.AuditLogJpaEntity;
import com.ziyara.backend.infrastructure.persistence.mapper.AuditLogMapper;
import com.ziyara.backend.infrastructure.persistence.repository.AuditLogJpaRepository;
import com.ziyara.backend.infrastructure.persistence.util.PageConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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

    @Override
    public PagedResult<AuditLog> findRecent(PageQuery pageQuery) {
        Pageable pageable = PageConverter.toPageable(pageQuery);
        return PageConverter.toPagedResult(auditLogJpaRepository.findAllByOrderByCreatedAtDesc(pageable), auditLogMapper::toDomainEntity);
    }

    @Override
    public PagedResult<AuditLog> findFiltered(String entityType,
                                              String action,
                                              UUID userId,
                                              LocalDateTime dateFrom,
                                              LocalDateTime dateTo,
                                              PageQuery pageQuery) {
        Pageable pageable = PageConverter.toPageable(pageQuery);
        return PageConverter.toPagedResult(auditLogJpaRepository.findFiltered(entityType, action, userId, dateFrom, dateTo, pageable), auditLogMapper::toDomainEntity);
    }
}
