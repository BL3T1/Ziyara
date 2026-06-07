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
        user.setUsername(entity.getUsername());
        user.setFirstName(entity.getFirstName());
        user.setLastName(entity.getLastName());
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
        user.setTokenVersion(entity.getTokenVersion() != null ? entity.getTokenVersion() : 0);
        user.setLastPasswordChange(entity.getLastPasswordChange());
        user.setPasswordExpiresAt(entity.getPasswordExpiresAt());
        user.setMfaEnabled(Boolean.TRUE.equals(entity.getMfaEnabled()));
        user.setMfaType(entity.getMfaType());
        user.setMfaSecretCipher(entity.getMfaSecretCipher());
        user.setMfaBackupCodesCipher(entity.getMfaBackupCodesCipher());
        user.setMfaLastUsedAt(entity.getMfaLastUsedAt());
        user.setMfaEnrolledAt(entity.getMfaEnrolledAt());
        user.setGdprConsentGiven(Boolean.TRUE.equals(entity.getGdprConsentGiven()));
        user.setGdprConsentDate(entity.getGdprConsentDate());
        user.setMarketingOptIn(Boolean.TRUE.equals(entity.getMarketingOptIn()));
        user.setRightToErasureRequested(Boolean.TRUE.equals(entity.getRightToErasureRequested()));
        user.setRightToErasureCompletedAt(entity.getRightToErasureCompletedAt());
        user.setMustChangePassword(Boolean.TRUE.equals(entity.getMustChangePassword()));
        user.setFcmToken(entity.getFcmToken());

        return user;
    }
    
    public UserJpaEntity toJpaEntity(User user) {
        if (user == null) {
            return null;
        }
        
        return UserJpaEntity.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
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
                .tokenVersion(user.getTokenVersion())
                .lastPasswordChange(user.getLastPasswordChange())
                .passwordExpiresAt(user.getPasswordExpiresAt())
                .mfaEnabled(user.isMfaEnabled())
                .mfaType(user.getMfaType())
                .mfaSecretCipher(user.getMfaSecretCipher())
                .mfaBackupCodesCipher(user.getMfaBackupCodesCipher())
                .mfaLastUsedAt(user.getMfaLastUsedAt())
                .mfaEnrolledAt(user.getMfaEnrolledAt())
                .gdprConsentGiven(user.isGdprConsentGiven())
                .gdprConsentDate(user.getGdprConsentDate())
                .marketingOptIn(user.isMarketingOptIn())
                .rightToErasureRequested(user.isRightToErasureRequested())
                .rightToErasureCompletedAt(user.getRightToErasureCompletedAt())
                .mustChangePassword(user.isMustChangePassword())
                .fcmToken(user.getFcmToken())
                .build();
    }
}
