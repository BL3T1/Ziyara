package com.ziyarah.infrastructure.persistence.adapter;

import com.ziyarah.domain.entity.Notification;
import com.ziyarah.domain.enums.NotificationStatus;
import com.ziyarah.domain.repository.NotificationRepository;
import com.ziyarah.infrastructure.persistence.entity.NotificationJpaEntity;
import com.ziyarah.infrastructure.persistence.mapper.NotificationMapper;
import com.ziyarah.infrastructure.persistence.repository.NotificationJpaRepository;
import lombok.RequiredArgsConstructor;
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
    public void deleteById(UUID id) {
        notificationJpaRepository.deleteById(id);
    }
}
