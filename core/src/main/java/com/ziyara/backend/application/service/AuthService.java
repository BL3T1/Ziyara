package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.AuthRequest;
import com.ziyara.backend.application.dto.AuthResponse;
import com.ziyara.backend.application.dto.request.*;
import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.enums.ProviderStatus;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.enums.UserStatus;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.infrastructure.security.crypto.PiiCryptoService;
import com.ziyara.backend.infrastructure.security.crypto.TotpService;
import com.ziyara.backend.infrastructure.persistence.entity.OtpVerificationJpaEntity;
import com.ziyara.backend.infrastructure.persistence.entity.PasswordResetTokenJpaEntity;
import com.ziyara.backend.infrastructure.persistence.repository.OtpVerificationJpaRepository;
import com.ziyara.backend.infrastructure.persistence.repository.PasswordResetTokenJpaRepository;
import com.ziyara.backend.infrastructure.persistence.repository.ProviderStaffJpaRepository;
import com.ziyara.backend.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Service: AuthService
 * Handles authentication business logic (commands = JPA).
 * Part of Clean Architecture - Application Layer
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final int PASSWORD_RESET_EXPIRY_MINUTES = 60;
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int OTP_LENGTH = 6;

    private final UserRepository userRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final ProviderStaffJpaRepository providerStaffJpaRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenJpaRepository passwordResetTokenRepository;
    private final OtpVerificationJpaRepository otpVerificationRepository;
    private final PasswordHistoryService passwordHistoryService;
    private final TotpService totpService;
    private final PiiCryptoService piiCryptoService;
    private final JwtTokenBlocklistService jwtTokenBlocklistService;
    private final PasswordPolicyService passwordPolicyService;
    private final AuthEmailNotificationService authEmailNotificationService;
    
    @Transactional
    public AuthResponse authenticate(AuthRequest request, String ipAddress) {
        String emailNorm = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase(Locale.ROOT);
        log.info("Authenticating user: {}", emailNorm);
        
        // Find user by email (normalized; staff creation stores lowercase)
        User user = userRepository.findByEmail(emailNorm)
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));
        
        // Check if user can login
        if (!user.getStatus().canLogin()) {
            throw new AuthenticationException("Account is not active. Status: " + user.getStatus());
        }

        if (isPartnerPortalRole(user.getRole())) {
            serviceProviderRepository.findByUserId(user.getId()).ifPresent(sp -> {
                if (sp.getStatus() != ProviderStatus.ACTIVE) {
                    throw new AuthenticationException(
                            "Partner account is not active yet. Provider status: " + sp.getStatus().name());
                }
            });
        }

        // Check if account is locked
        if (user.isLocked()) {
            throw new AuthenticationException("Account is temporarily locked. Please try again later.");
        }
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            // Increment failed attempts
            user.incrementFailedLoginAttempts();
            userRepository.save(user);
            throw new AuthenticationException("Invalid email or password");
        }

        if (user.isMfaEnabled()) {
            String code = request.getMfaCode() == null ? "" : request.getMfaCode().trim();
            if (code.length() != 6) {
                throw new AuthenticationException("MFA code required");
            }
            String secretCipher = user.getMfaSecretCipher();
            if (secretCipher == null || secretCipher.isBlank()) {
                throw new AuthenticationException("MFA is misconfigured for this account");
            }
            String secret = piiCryptoService.decrypt(secretCipher);
            if (!totpService.verify(secret, code)) {
                throw new AuthenticationException("Invalid MFA code");
            }
            user.setMfaLastUsedAt(LocalDateTime.now());
        }
        
        // Record successful login
        user.recordSuccessfulLogin(ipAddress);
        userRepository.save(user);
        
        UUID providerScope = resolveProviderScopeForJwt(user);
        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user, providerScope);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        log.info("User authenticated successfully: {}", user.getEmail());
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
    
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        revokeTokenIfPresent(accessToken);
        revokeTokenIfPresent(refreshToken);
        log.info("User logged out");
    }

    public long accessTokenTtlSeconds() {
        return jwtService.getExpirationTime();
    }

    public long refreshTokenTtlSeconds() {
        return jwtService.getRefreshExpirationSeconds();
    }

    private void revokeTokenIfPresent(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            if (!jwtService.validateToken(token)) {
                return;
            }
            String jti = jwtService.extractJti(token);
            if (jti != null) {
                jwtTokenBlocklistService.revokeUntilExpiry(jti, jwtService.extractExpirationInstant(token));
            }
        } catch (Exception e) {
            log.debug("Logout revoke skipped: {}", e.getMessage());
        }
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtService.validateToken(refreshToken)) {
            throw new AuthenticationException("Invalid refresh token");
        }

        String oldJti = jwtService.extractJti(refreshToken);
        if (oldJti != null && jwtTokenBlocklistService.isRevoked(oldJti)) {
            throw new AuthenticationException("Refresh token is no longer valid");
        }

        String userId = jwtService.extractUserId(refreshToken);
        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new AuthenticationException("User not found"));

        int tv = jwtService.extractTokenVersion(refreshToken);
        if (tv != user.getTokenVersion()) {
            throw new AuthenticationException("Refresh token is no longer valid");
        }

        if (!user.isActive()) {
            throw new AuthenticationException("Account is not active");
        }

        if (oldJti != null) {
            jwtTokenBlocklistService.revokeUntilExpiry(oldJti, jwtService.extractExpirationInstant(refreshToken));
        }

        UUID providerScope = resolveProviderScopeForJwt(user);
        String newAccessToken = jwtService.generateAccessToken(user, providerScope);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    @Transactional
    public void register(RegisterRequest request) {
        passwordPolicyService.assertAcceptable(request.getPassword());
        if (isProviderPortalRole(request.getRole())) {
            throw new IllegalArgumentException(
                    "Provider accounts must be created from provider onboarding so the provider profile is created and linked.");
        }
        String emailNorm = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(emailNorm)) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (request.getPhone() != null && !request.getPhone().isBlank() && userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Phone already registered");
        }
        User user = new User();
        // Do not set id; let JPA persist generate it so save() uses persist() not merge()
        user.setEmail(emailNorm);
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        // Self-registered customers are ACTIVE so they can log in immediately; other flows can require verification
        user.setStatus(request.getRole() == com.ziyara.backend.domain.enums.UserRole.CUSTOMER
                ? UserStatus.ACTIVE
                : UserStatus.PENDING_VERIFICATION);
        userRepository.save(user);
        log.info("User registered: {}", user.getEmail());
        storeAndEmailOtp(emailNorm, true);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String emailNorm = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmail(emailNorm).orElse(null);
        if (user == null) {
            log.info("Forgot password requested for unknown email: {}", emailNorm);
            return;
        }
        passwordResetTokenRepository.deleteByUserId(user.getId());
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(PASSWORD_RESET_EXPIRY_MINUTES * 60L);
        passwordResetTokenRepository.save(PasswordResetTokenJpaEntity.builder()
                .userId(user.getId())
                .token(token)
                .expiresAt(expiresAt)
                .build());
        authEmailNotificationService.sendPasswordReset(user.getEmail(), token);
        log.info("Password reset token generated for {}", user.getEmail());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        passwordPolicyService.assertAcceptable(request.getNewPassword());
        var tokenEntity = passwordResetTokenRepository.findByTokenAndExpiresAtAfter(
                request.getToken(), Instant.now()).orElseThrow(() -> new AuthenticationException("Invalid or expired reset token"));
        User user = userRepository.findById(tokenEntity.getUserId())
                .orElseThrow(() -> new AuthenticationException("User not found"));
        passwordHistoryService.assertPasswordNotReused(user.getId(), request.getNewPassword(), passwordEncoder, user.getPasswordHash());
        passwordHistoryService.recordPasswordRotation(user.getId(), user.getPasswordHash());
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setLastPasswordChange(LocalDateTime.now());
        user.incrementTokenVersion();
        userRepository.save(user);
        passwordResetTokenRepository.delete(tokenEntity);
        log.info("Password reset completed for user: {}", user.getEmail());
    }

    @Transactional
    public void sendOtp(OtpSendRequest request) {
        storeAndEmailOtp(request.getEmailOrPhone(), false);
    }

    @Transactional
    public void verifyOtp(OtpVerifyRequest request) {
        String key = request.getEmailOrPhone() == null ? "" : request.getEmailOrPhone().trim();
        if (key.contains("@")) {
            key = normalizeEmail(key);
        }
        var entity = otpVerificationRepository.findByEmailOrPhoneAndOtpAndExpiresAtAfter(
                key, request.getOtp(), Instant.now())
                .orElseThrow(() -> new AuthenticationException("Invalid or expired OTP"));
        otpVerificationRepository.delete(entity);
        log.info("OTP verified for {}", key);
    }

    /**
     * Custom exception for authentication errors
     */
    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Persists a fresh OTP and emails it when the destination looks like an email and mail is enabled.
     *
     * @param signupCopy use welcome/signup wording in the email
     */
    private void storeAndEmailOtp(String emailOrPhone, boolean signupCopy) {
        String dest = emailOrPhone == null ? "" : emailOrPhone.trim();
        if (dest.isEmpty()) {
            return;
        }
        if (dest.contains("@")) {
            dest = normalizeEmail(dest);
        }
        String otp = String.format("%0" + OTP_LENGTH + "d", new java.util.Random().nextInt((int) Math.pow(10, OTP_LENGTH)));
        Instant expiresAt = Instant.now().plusSeconds(OTP_EXPIRY_MINUTES * 60L);
        otpVerificationRepository.deleteByEmailOrPhone(dest);
        otpVerificationRepository.save(OtpVerificationJpaEntity.builder()
                .emailOrPhone(dest)
                .otp(otp)
                .expiresAt(expiresAt)
                .build());
        if (dest.contains("@")) {
            if (signupCopy) {
                authEmailNotificationService.sendSignupOtp(dest, otp);
            } else {
                authEmailNotificationService.sendOtpCode(dest, otp);
            }
        }
        log.info("OTP issued for {}", dest);
        log.debug("OTP value for {}: {}", dest, otp);
    }

    private static boolean isPartnerPortalRole(UserRole role) {
        return isProviderPortalRole(role);
    }

    private static boolean isProviderPortalRole(UserRole role) {
        return role == UserRole.PROVIDER_MANAGER
                || role == UserRole.PROVIDER_FINANCE
                || role == UserRole.PROVIDER_STAFF
                || role == UserRole.TAXI_OPERATOR;
    }

    /**
     * Primary provider id for portal users (owner via {@code created_by} link, else first staff assignment).
     */
    private UUID resolveProviderScopeForJwt(User user) {
        Optional<ServiceProvider> owned = serviceProviderRepository.findByUserId(user.getId());
        if (owned.isPresent()) {
            return owned.get().getId();
        }
        return providerStaffJpaRepository.findByUserId(user.getId())
                .map(link -> link.getProviderId())
                .orElse(null);
    }
}
