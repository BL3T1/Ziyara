package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper: UserMapper
 * Maps between domain User entity and JPA UserJpaEntity
 */
@Component
public class UserMapper {
    
    public User toDomainEntity(UserJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        User user = new User();
        user.setId(entity.getId());
        user.setEmail(entity.getEmail());
        user.setPhone(entity.getPhone());
        user.setPasswordHash(entity.getPasswordHash());
        user.setRole(entity.getRole());
        user.setStatus(entity.getStatus());
        user.setEmailVerified(entity.getEmailVerified() != null && entity.getEmailVerified());
        user.setPhoneVerified(entity.getPhoneVerified() != null && entity.getPhoneVerified());
        user.setFailedLoginAttempts(entity.getFailedLoginAttempts() != null ? entity.getFailedLoginAttempts() : 0);
        user.setLockedUntil(entity.getLockedUntil());
        user.setLastLoginAt(entity.getLastLoginAt());
        user.setLastLoginIp(entity.getLastLoginIp());
        user.setCreatedAt(entity.getCreatedAt());
        user.setUpdatedAt(entity.getUpdatedAt());
        user.setDeletedAt(entity.getDeletedAt());
        
        return user;
    }
    
    public UserJpaEntity toJpaEntity(User user) {
        if (user == null) {
            return null;
        }
        
        return UserJpaEntity.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .passwordHash(user.getPasswordHash())
                .role(user.getRole())
                .status(user.getStatus())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .lockedUntil(user.getLockedUntil())
                .lastLoginAt(user.getLastLoginAt())
                .lastLoginIp(user.getLastLoginIp())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .deletedAt(user.getDeletedAt())
                .build();
    }
}
