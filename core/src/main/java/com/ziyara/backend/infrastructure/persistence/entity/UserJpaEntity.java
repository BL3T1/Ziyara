package com.ziyara.backend.infrastructure.persistence.entity;

import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.enums.UserStatus;
import com.ziyara.backend.infrastructure.persistence.converter.UserRoleAttributeConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity: UserJpaEntity
 * Infrastructure layer representation of User
 * Maps to 'sys_users' table in PostgreSQL
 */
@Entity
@Table(name = "sys_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE sys_users SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class UserJpaEntity implements Persistable<UUID> {

    @Override
    public UUID getId() {
        return id;
    }

    /**
     * Treat as new when createdAt is null so save() uses persist() instead of merge() for new users.
     */
    @Override
    public boolean isNew() {
        return createdAt == null;
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "username", unique = true)
    private String username;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "phone", unique = true)
    private String phone;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @Convert(converter = UserRoleAttributeConverter.class)
    @Column(name = "role", nullable = false)
    private UserRole role;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;
    
    @Column(name = "email_verified")
    private Boolean emailVerified = false;
    
    @Column(name = "phone_verified")
    private Boolean phoneVerified = false;
    
    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;
    
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;
    
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "token_version", nullable = false)
    private Integer tokenVersion = 0;

    @Column(name = "last_password_change")
    private LocalDateTime lastPasswordChange;

    @Column(name = "password_expires_at")
    private LocalDateTime passwordExpiresAt;

    @Column(name = "mfa_enabled", nullable = false)
    private Boolean mfaEnabled = false;

    @Column(name = "mfa_type", length = 20)
    private String mfaType;

    @Column(name = "mfa_secret_cipher", columnDefinition = "TEXT")
    private String mfaSecretCipher;

    @Column(name = "mfa_backup_codes_cipher", columnDefinition = "TEXT")
    private String mfaBackupCodesCipher;

    @Column(name = "mfa_last_used_at")
    private LocalDateTime mfaLastUsedAt;

    @Column(name = "mfa_enrolled_at")
    private LocalDateTime mfaEnrolledAt;

    @Column(name = "gdpr_consent_given", nullable = false)
    private Boolean gdprConsentGiven = false;

    @Column(name = "gdpr_consent_date")
    private LocalDateTime gdprConsentDate;

    @Column(name = "marketing_opt_in", nullable = false)
    private Boolean marketingOptIn = false;

    @Column(name = "right_to_erasure_requested", nullable = false)
    private Boolean rightToErasureRequested = false;

    @Column(name = "right_to_erasure_completed_at")
    private LocalDateTime rightToErasureCompletedAt;

    @Column(name = "must_change_password", nullable = false)
    private Boolean mustChangePassword = false;

    @Column(name = "fcm_token", length = 512)
    private String fcmToken;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
