package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.AuthRequest;
import com.ziyara.backend.application.dto.AuthResponse;
import com.ziyara.backend.application.dto.request.*;
import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.enums.ProviderStatus;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.enums.UserStatus;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.infrastructure.persistence.entity.OtpVerificationJpaEntity;
import com.ziyara.backend.infrastructure.persistence.entity.PasswordResetTokenJpaEntity;
import com.ziyara.backend.infrastructure.persistence.repository.OtpVerificationJpaRepository;
import com.ziyara.backend.infrastructure.persistence.repository.PasswordResetTokenJpaRepository;
import com.ziyara.backend.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
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
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenJpaRepository passwordResetTokenRepository;
    private final OtpVerificationJpaRepository otpVerificationRepository;
    
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
        
        // Record successful login
        user.recordSuccessfulLogin(ipAddress);
        userRepository.save(user);
        
        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
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
    public void logout(String token) {
        // Token invalidation logic (could use Redis blacklist)
        log.info("User logged out");
    }
    
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtService.validateToken(refreshToken)) {
            throw new AuthenticationException("Invalid refresh token");
        }
        
        String userId = jwtService.extractUserId(refreshToken);
        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new AuthenticationException("User not found"));
        
        if (!user.isActive()) {
            throw new AuthenticationException("Account is not active");
        }
        
        String newAccessToken = jwtService.generateAccessToken(user);
        
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (request.getPhone() != null && !request.getPhone().isBlank() && userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Phone already registered");
        }
        User user = new User();
        // Do not set id; let JPA persist generate it so save() uses persist() not merge()
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        // Self-registered customers are ACTIVE so they can log in immediately; other flows can require verification
        user.setStatus(request.getRole() == com.ziyara.backend.domain.enums.UserRole.CUSTOMER
                ? UserStatus.ACTIVE
                : UserStatus.PENDING_VERIFICATION);
        userRepository.save(user);
        log.info("User registered: {}", user.getEmail());
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            log.info("Forgot password requested for unknown email: {}", request.getEmail());
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
        log.info("Password reset token generated for {} (stub: token not sent by email)", request.getEmail());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        var tokenEntity = passwordResetTokenRepository.findByTokenAndExpiresAtAfter(
                request.getToken(), Instant.now()).orElseThrow(() -> new AuthenticationException("Invalid or expired reset token"));
        User user = userRepository.findById(tokenEntity.getUserId())
                .orElseThrow(() -> new AuthenticationException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        passwordResetTokenRepository.delete(tokenEntity);
        log.info("Password reset completed for user: {}", user.getEmail());
    }

    @Transactional
    public void sendOtp(OtpSendRequest request) {
        String otp = String.format("%0" + OTP_LENGTH + "d", new java.util.Random().nextInt((int) Math.pow(10, OTP_LENGTH)));
        Instant expiresAt = Instant.now().plusSeconds(OTP_EXPIRY_MINUTES * 60L);
        otpVerificationRepository.deleteByEmailOrPhone(request.getEmailOrPhone());
        otpVerificationRepository.save(OtpVerificationJpaEntity.builder()
                .emailOrPhone(request.getEmailOrPhone())
                .otp(otp)
                .expiresAt(expiresAt)
                .build());
        log.info("OTP sent for {} (stub: OTP not delivered, code={})", request.getEmailOrPhone(), otp);
    }

    @Transactional
    public void verifyOtp(OtpVerifyRequest request) {
        var entity = otpVerificationRepository.findByEmailOrPhoneAndOtpAndExpiresAtAfter(
                request.getEmailOrPhone(), request.getOtp(), Instant.now())
                .orElseThrow(() -> new AuthenticationException("Invalid or expired OTP"));
        otpVerificationRepository.delete(entity);
        log.info("OTP verified for {}", request.getEmailOrPhone());
    }

    /**
     * Custom exception for authentication errors
     */
    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }

    private static boolean isPartnerPortalRole(UserRole role) {
        return role == UserRole.PROVIDER_MANAGER || role == UserRole.PROVIDER_FINANCE
                || role == UserRole.PROVIDER_STAFF || role == UserRole.TAXI_OPERATOR;
    }
}
