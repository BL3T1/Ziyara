package com.ziyarah.infrastructure.persistence.mapper;

import com.ziyarah.domain.entity.Notification;
import com.ziyarah.infrastructure.persistence.entity.NotificationJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper: NotificationMapper
 */
@Component
public class NotificationMapper {
    
    public Notification toDomainEntity(NotificationJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        Notification notification = new Notification();
        notification.setId(entity.getId());
        notification.setUserId(entity.getUserId());
        notification.setType(entity.getType());
        notification.setChannel(entity.getChannel());
        notification.setStatus(entity.getStatus());
        notification.setTitle(entity.getTitle());
        notification.setMessage(entity.getMessage());
        notification.setTemplateName(entity.getTemplateName());
        notification.setMetadata(entity.getMetadata());
        notification.setSentAt(entity.getSentAt());
        notification.setReadAt(entity.getReadAt());
        notification.setCreatedAt(entity.getCreatedAt());
        notification.setUpdatedAt(entity.getUpdatedAt());
        
        return notification;
    }
    
    public NotificationJpaEntity toJpaEntity(Notification notification) {
        if (notification == null) {
            return null;
        }
        
        return NotificationJpaEntity.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .channel(notification.getChannel())
                .status(notification.getStatus())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .templateName(notification.getTemplateName())
                .metadata(notification.getMetadata())
                .sentAt(notification.getSentAt())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }
}
