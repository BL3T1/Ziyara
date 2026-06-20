package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.common.PageQuery;
import com.ziyara.backend.domain.common.PagedResult;
import com.ziyara.backend.domain.entity.Notification;
import com.ziyara.backend.domain.enums.NotificationStatus;
import com.ziyara.backend.domain.repository.NotificationRepository;
import com.ziyara.backend.infrastructure.persistence.entity.NotificationJpaEntity;
import com.ziyara.backend.infrastructure.persistence.mapper.NotificationMapper;
import com.ziyara.backend.infrastructure.persistence.repository.NotificationJpaRepository;
import com.ziyara.backend.infrastructure.persistence.util.PageConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository Adapter: NotificationRepositoryAdapter
 */
@Component
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepository {
    
    private final NotificationJpaRepository notificationJpaRepository;
    private final NotificationMapper notificationMapper;
    
    @Override
    public Notification save(Notification notification) {
        NotificationJpaEntity entity = notificationMapper.toJpaEntity(notification);
        NotificationJpaEntity savedEntity = notificationJpaRepository.save(entity);
        return notificationMapper.toDomainEntity(savedEntity);
    }
    
    @Override
    public Optional<Notification> findById(UUID id) {
        return notificationJpaRepository.findById(id)
                .map(notificationMapper::toDomainEntity);
    }
    
    @Override
    public List<Notification> findByUserId(UUID userId) {
        return notificationJpaRepository.findByUserId(userId).stream()
                .map(notificationMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public PagedResult<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, PageQuery pageQuery) {
        Pageable pageable = PageConverter.toPageable(pageQuery);
        return PageConverter.toPagedResult(notificationJpaRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable), notificationMapper::toDomainEntity);
    }

    @Override
    public long countByUserIdAndReadAtIsNull(UUID userId) {
        return notificationJpaRepository.countByUserIdAndReadAtIsNull(userId);
    }
    
    @Override
    public List<Notification> findByUserIdAndStatus(UUID userId, NotificationStatus status) {
        return notificationJpaRepository.findByUserIdAndStatus(userId, status).stream()
                .map(notificationMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Notification> findByStatus(NotificationStatus status) {
        return notificationJpaRepository.findByStatus(status).stream()
                .map(notificationMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public long countByUserIdAndStatus(UUID userId, NotificationStatus status) {
        return notificationJpaRepository.countByUserIdAndStatus(userId, status);
    }
    
    @Override
    public int markAllReadByUserId(UUID userId) {
        return notificationJpaRepository.markAllReadByUserId(userId);
    }

    @Override
    public void deleteById(UUID id) {
        notificationJpaRepository.deleteById(id);
    }
}
