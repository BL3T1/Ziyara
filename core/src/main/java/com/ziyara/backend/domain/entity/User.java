package com.ziyara.backend.domain.entity;

import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain Entity: User
 * Core user entity representing all system users (customers and staff)
 * No framework dependencies - pure Java
 */
public class User {

    private UUID id;
    private String email;
    private String phone;
    private String passwordHash;
    private UserRole role;
    private UserStatus status;
    private boolean emailVerified;
    private boolean phoneVerified;
    private int failedLoginAttempts;
    private LocalDateTime lockedUntil;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    /** Bumped on password change; embedded in JWT as {@code tv} to invalidate old tokens. */
    private int tokenVersion;

    private LocalDateTime lastPasswordChange;
    private LocalDateTime passwordExpiresAt;

    private boolean mfaEnabled;
    private String mfaType;
    private String mfaSecretCipher;
    private String mfaBackupCodesCipher;
    private LocalDateTime mfaLastUsedAt;
    private LocalDateTime mfaEnrolledAt;

    private boolean gdprConsentGiven;
    private LocalDateTime gdprConsentDate;
    private boolean marketingOptIn;
    private boolean rightToErasureRequested;
    private LocalDateTime rightToErasureCompletedAt;

    // Domain behavior methods
    public boolean isActive() {
        return status == UserStatus.ACTIVE && deletedAt == null;
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    public void lockAccount(int lockoutDurationMinutes) {
        this.lockedUntil = LocalDateTime.now().plusMinutes(lockoutDurationMinutes);
    }

    public void recordSuccessfulLogin(String ipAddress) {
        this.lastLoginAt = LocalDateTime.now();
        this.lastLoginIp = ipAddress;
        resetFailedLoginAttempts();
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public void verifyPhone() {
        this.phoneVerified = true;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void freeze() {
        this.status = UserStatus.FROZEN;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.status = UserStatus.DELETED;
    }

    // Constructors
    public User() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.tokenVersion = 0;
    }

    public User(UUID id, String email, String phone, String passwordHash, UserRole role) {
        this();
        this.id = id;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = UserStatus.PENDING_VERIFICATION;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isPhoneVerified() {
        return phoneVerified;
    }

    public void setPhoneVerified(boolean phoneVerified) {
        this.phoneVerified = phoneVerified;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }

    public void setTokenVersion(int tokenVersion) {
        this.tokenVersion = tokenVersion;
    }

    public void incrementTokenVersion() {
        this.tokenVersion++;
    }

    public LocalDateTime getLastPasswordChange() {
        return lastPasswordChange;
    }

    public void setLastPasswordChange(LocalDateTime lastPasswordChange) {
        this.lastPasswordChange = lastPasswordChange;
    }

    public LocalDateTime getPasswordExpiresAt() {
        return passwordExpiresAt;
    }

    public void setPasswordExpiresAt(LocalDateTime passwordExpiresAt) {
        this.passwordExpiresAt = passwordExpiresAt;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public String getMfaType() {
        return mfaType;
    }

    public void setMfaType(String mfaType) {
        this.mfaType = mfaType;
    }

    public String getMfaSecretCipher() {
        return mfaSecretCipher;
    }

    public void setMfaSecretCipher(String mfaSecretCipher) {
        this.mfaSecretCipher = mfaSecretCipher;
    }

    public String getMfaBackupCodesCipher() {
        return mfaBackupCodesCipher;
    }

    public void setMfaBackupCodesCipher(String mfaBackupCodesCipher) {
        this.mfaBackupCodesCipher = mfaBackupCodesCipher;
    }

    public LocalDateTime getMfaLastUsedAt() {
        return mfaLastUsedAt;
    }

    public void setMfaLastUsedAt(LocalDateTime mfaLastUsedAt) {
        this.mfaLastUsedAt = mfaLastUsedAt;
    }

    public LocalDateTime getMfaEnrolledAt() {
        return mfaEnrolledAt;
    }

    public void setMfaEnrolledAt(LocalDateTime mfaEnrolledAt) {
        this.mfaEnrolledAt = mfaEnrolledAt;
    }

    public boolean isGdprConsentGiven() {
        return gdprConsentGiven;
    }

    public void setGdprConsentGiven(boolean gdprConsentGiven) {
        this.gdprConsentGiven = gdprConsentGiven;
    }

    public LocalDateTime getGdprConsentDate() {
        return gdprConsentDate;
    }

    public void setGdprConsentDate(LocalDateTime gdprConsentDate) {
        this.gdprConsentDate = gdprConsentDate;
    }

    public boolean isMarketingOptIn() {
        return marketingOptIn;
    }

    public void setMarketingOptIn(boolean marketingOptIn) {
        this.marketingOptIn = marketingOptIn;
    }

    public boolean isRightToErasureRequested() {
        return rightToErasureRequested;
    }

    public void setRightToErasureRequested(boolean rightToErasureRequested) {
        this.rightToErasureRequested = rightToErasureRequested;
    }

    public LocalDateTime getRightToErasureCompletedAt() {
        return rightToErasureCompletedAt;
    }

    public void setRightToErasureCompletedAt(LocalDateTime rightToErasureCompletedAt) {
        this.rightToErasureCompletedAt = rightToErasureCompletedAt;
    }
}
