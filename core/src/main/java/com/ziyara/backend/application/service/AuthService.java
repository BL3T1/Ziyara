package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.AuthRequest;
import com.ziyara.backend.application.dto.AuthResponse;
import com.ziyara.backend.application.dto.request.*;
import com.ziyara.backend.domain.entity.Customer;
import com.ziyara.backend.domain.entity.OtpVerification;
import com.ziyara.backend.domain.entity.PasswordResetToken;
import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.enums.ProviderStatus;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.enums.UserStatus;
import com.ziyara.backend.domain.repository.CustomerRepository;
import com.ziyara.backend.domain.repository.OtpVerificationRepository;
import com.ziyara.backend.domain.repository.PasswordResetTokenRepository;
import com.ziyara.backend.domain.repository.ProviderStaffRepository;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.infrastructure.security.crypto.PiiCryptoService;
import com.ziyara.backend.infrastructure.security.crypto.TotpService;
import com.ziyara.backend.infrastructure.security.JwtService;
import com.ziyara.backend.application.annotation.Audited;
import com.ziyara.backend.application.exception.MfaEnrollmentRequiredException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

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

    /**
     * Comma-separated {@link UserRole} names that require TOTP enrollment before login.
     * Controlled by {@code ZIYARA_SECURITY_MFA_REQUIRED_ROLES}. Empty = no enforcement (dev default).
     */
    @Value("${ziyara.security.mfa-required-roles:}")
    private String mfaRequiredRolesRaw;

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final ProviderStaffRepository providerStaffRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final PasswordHistoryService passwordHistoryService;
    private final TotpService totpService;
    private final PiiCryptoService piiCryptoService;
    private final JwtTokenBlocklistService jwtTokenBlocklistService;
    private final PasswordPolicyService passwordPolicyService;
    private final AuthEmailNotificationService authEmailNotificationService;
    private final LoginAuditService loginAuditService;

    // Non-final: requires @Qualifier which is incompatible with @RequiredArgsConstructor.
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.beans.factory.annotation.Qualifier("bcryptExecutor")
    private Executor bcryptExecutor;

    @Audited(action = "USER_LOGIN", entityType = "Auth")
    @Transactional
    public AuthResponse authenticate(AuthRequest request, String ipAddress) {
        String identifier = request.getEmail() == null ? "" : request.getEmail().trim();
        log.info("Authenticating user: {}", identifier);

        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier.toLowerCase(Locale.ROOT)))
                .orElseThrow(() -> new AuthenticationException("Invalid credentials"));

        if (!user.getStatus().canLogin()) {
            throw new AuthenticationException("Account is not active. Status: " + user.getStatus());
        }

        // Fix 5: capture once — reused below for JWT scope to avoid a duplicate DB round-trip.
        Optional<ServiceProvider> ownedProvider = serviceProviderRepository.findByUserId(user.getId());
        ownedProvider.ifPresent(sp -> {
            if (sp.getStatus() != ProviderStatus.ACTIVE) {
                throw new AuthenticationException(
                        "Partner account is not active yet. Provider status: " + sp.getStatus().name());
            }
            if (sp.isExpired()) {
                throw new AuthenticationException(
                        "Partner account has expired. Please contact your administrator to renew.");
            }
        });

        if (user.isLocked()) {
            throw new AuthenticationException("Account is temporarily locked. Please try again later.");
        }

        // Fix 4: BCrypt runs on a bounded pool so it cannot pin all virtual-thread carrier threads.
        if (!verifyPassword(request.getPassword(), user.getPasswordHash())) {
            user.incrementFailedLoginAttempts();
            userRepository.save(user);
            throw new AuthenticationException("Invalid credentials");
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

        // Enforce TOTP enrollment for roles listed in ZIYARA_SECURITY_MFA_REQUIRED_ROLES
        if (!user.isMfaEnabled() && mfaRequiredRoles().contains(user.getRole())) {
            throw new MfaEnrollmentRequiredException(
                    "Your role (" + user.getRole() + ") requires TOTP enrollment before login. " +
                    "Please enrol via POST /users/me/mfa/enroll/start using a temporary session, " +
                    "or contact your administrator.");
        }

        // Fix 5: reuse the already-fetched provider Optional instead of calling the repo again.
        UUID providerScope = ownedProvider.map(ServiceProvider::getId).orElseGet(() ->
                providerStaffRepository.findByUserId(user.getId())
                        .map(link -> link.getProviderId())
                        .orElse(null));
        String accessToken = jwtService.generateAccessToken(user, providerScope);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Fix 2: write lastLoginAt / reset failedLoginAttempts after the JWT is ready — client
        // gets the token immediately without waiting for the users-table row lock.
        loginAuditService.recordSuccessfulLogin(user.getId(), ipAddress);

        log.info("User authenticated successfully: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(buildFullName(user))
                .role(user.getRole())
                .mustChangePassword(user.isMustChangePassword())
                .hasPortalAccess(providerScope != null)
                .build();
    }

    @Audited(action = "USER_LOGOUT", entityType = "Auth")
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
        } catch (RuntimeException e) {
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
        User user = userRepository.findById(UUID.fromString(userId))
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
                .fullName(buildFullName(user))
                .role(user.getRole())
                .mustChangePassword(user.isMustChangePassword())
                .hasPortalAccess(providerScope != null)
                .build();
    }

    @Audited(action = "USER_REGISTER", entityType = "Auth")
    @Transactional
    public void register(RegisterRequest request) {
        passwordPolicyService.assertAcceptable(request.getPassword());
        UserRole role = request.getRole() != null ? request.getRole() : UserRole.CUSTOMER;
        if (role != UserRole.CUSTOMER) {
            throw new IllegalArgumentException(
                    "Public self-registration is for customer accounts only.");
        }
        String emailNorm = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(emailNorm)) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()
                && userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Phone already registered");
        }
        User user = new User();
        user.setEmail(emailNorm);
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setStatus(role == UserRole.CUSTOMER ? UserStatus.ACTIVE : UserStatus.PENDING_VERIFICATION);
        userRepository.save(user);

        if (role == UserRole.CUSTOMER) {
            Customer customer = new Customer(user.getId(), request.getFirstName(), request.getLastName());
            customer.setDateOfBirth(request.getDateOfBirth());
            customer.setNationality(request.getNationality());
            customerRepository.save(customer);
        }

        log.info("User registered: {}", user.getEmail());
        storeAndEmailOtp(emailNorm, true);
    }

    @Audited(action = "PASSWORD_FORGOT", entityType = "Auth")
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
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(user.getId());
        resetToken.setToken(token);
        resetToken.setExpiresAt(expiresAt);
        passwordResetTokenRepository.save(resetToken);
        authEmailNotificationService.sendPasswordReset(user.getEmail(), token);
        log.info("Password reset token generated for {}", user.getEmail());
    }

    @Audited(action = "PASSWORD_RESET", entityType = "Auth")
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        passwordPolicyService.assertAcceptable(request.getNewPassword());
        PasswordResetToken tokenEntity = passwordResetTokenRepository
                .findValidByToken(request.getToken(), Instant.now())
                .orElseThrow(() -> new AuthenticationException("Invalid or expired reset token"));
        User user = userRepository.findById(tokenEntity.getUserId())
                .orElseThrow(() -> new AuthenticationException("User not found"));
        passwordHistoryService.assertPasswordNotReused(user.getId(), request.getNewPassword(), passwordEncoder, user.getPasswordHash());
        passwordHistoryService.recordPasswordRotation(user.getId(), user.getPasswordHash());
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setLastPasswordChange(LocalDateTime.now());
        user.incrementTokenVersion();
        userRepository.save(user);
        // Delete all tokens for the user — the validated token is now consumed
        passwordResetTokenRepository.deleteByUserId(tokenEntity.getUserId());
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
        OtpVerification entity = otpVerificationRepository
                .findValidByEmailOrPhoneAndOtp(key, request.getOtp(), Instant.now())
                .orElseThrow(() -> new AuthenticationException("Invalid or expired OTP"));
        otpVerificationRepository.deleteByEmailOrPhone(entity.getEmailOrPhone());
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
        String otp = String.format("%0" + OTP_LENGTH + "d",
                new java.util.Random().nextInt((int) Math.pow(10, OTP_LENGTH)));
        Instant expiresAt = Instant.now().plusSeconds(OTP_EXPIRY_MINUTES * 60L);
        otpVerificationRepository.deleteByEmailOrPhone(dest);
        OtpVerification otpEntity = new OtpVerification();
        otpEntity.setEmailOrPhone(dest);
        otpEntity.setOtp(otp);
        otpEntity.setExpiresAt(expiresAt);
        otpVerificationRepository.save(otpEntity);
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

    /**
     * Parses {@code ZIYARA_SECURITY_MFA_REQUIRED_ROLES} into a set of {@link UserRole} values.
     * Unknown names (old enum values) are silently ignored so old configs don't crash.
     * Returns an empty set when the property is blank (dev default — no enforcement).
     */
    private Set<UserRole> mfaRequiredRoles() {
        if (mfaRequiredRolesRaw == null || mfaRequiredRolesRaw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(mfaRequiredRolesRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .flatMap(s -> {
                    try {
                        return java.util.stream.Stream.of(UserRole.valueOf(s));
                    } catch (IllegalArgumentException e) {
                        return java.util.stream.Stream.empty();
                    }
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Generates an access token scoped to the given provider for the requesting admin.
     * The admin retains their own identity and permissions (SUPER_ADMIN has portal:access via V17 seed),
     * while the {@code pid} JWT claim restricts data visibility to that provider via RLS.
     * Bypasses all normal auth checks and sends no notifications.
     * Restricted to SUPER_ADMIN callers (enforced at the controller level).
     */
    @Transactional(readOnly = true)
    public AuthResponse generateAdminProviderToken(UUID providerId, UUID adminUserId) {
        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerId));
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found: " + adminUserId));
        String accessToken = jwtService.generateAccessToken(admin, provider.getId());
        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .userId(admin.getId())
                .email(admin.getEmail())
                .fullName(buildFullName(admin))
                .role(admin.getRole())
                .hasPortalAccess(true)
                .build();
    }

    private static String buildFullName(User user) {
        String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String last  = user.getLastName()  != null ? user.getLastName().trim()  : "";
        String full  = (first + " " + last).trim();
        return full.isEmpty() ? null : full;
    }

    /**
     * Primary provider id for portal users (owner via userId link, else first staff assignment).
     * Used by refreshToken() where no pre-fetched Optional is available.
     */
    private UUID resolveProviderScopeForJwt(User user) {
        Optional<ServiceProvider> owned = serviceProviderRepository.findByUserId(user.getId());
        if (owned.isPresent()) {
            return owned.get().getId();
        }
        return providerStaffRepository.findByUserId(user.getId())
                .map(link -> link.getProviderId())
                .orElse(null);
    }

    /**
     * Runs BCrypt on a bounded executor so it cannot monopolise all JVM carrier threads.
     * The calling virtual thread suspends (yielding its carrier) until the result is ready.
     */
    private boolean verifyPassword(String raw, String hash) {
        try {
            return CompletableFuture
                    .supplyAsync(() -> passwordEncoder.matches(raw, hash), bcryptExecutor)
                    .get(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException | TimeoutException e) {
            log.warn("BCrypt verification timed out or failed: {}", e.getMessage());
            return false;
        }
    }
}
