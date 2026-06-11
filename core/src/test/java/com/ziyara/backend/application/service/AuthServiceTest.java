package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.AuthRequest;
import com.ziyara.backend.application.dto.AuthResponse;
import com.ziyara.backend.application.dto.request.ForgotPasswordRequest;
import com.ziyara.backend.application.dto.request.ResetPasswordRequest;
import com.ziyara.backend.application.exception.MfaEnrollmentRequiredException;
import com.ziyara.backend.domain.entity.PasswordResetToken;
import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.enums.UserStatus;
import com.ziyara.backend.domain.repository.OtpVerificationRepository;
import com.ziyara.backend.domain.repository.PasswordResetTokenRepository;
import com.ziyara.backend.domain.repository.ProviderStaffRepository;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.infrastructure.security.JwtService;
import com.ziyara.backend.infrastructure.security.crypto.PiiCryptoService;
import com.ziyara.backend.infrastructure.security.crypto.TotpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock ServiceProviderRepository serviceProviderRepository;
    @Mock ProviderStaffRepository providerStaffRepository;
    @Mock JwtService jwtService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock OtpVerificationRepository otpVerificationRepository;
    @Mock PasswordHistoryService passwordHistoryService;
    @Mock TotpService totpService;
    @Mock PiiCryptoService piiCryptoService;
    @Mock JwtTokenBlocklistService jwtTokenBlocklistService;
    @Mock PasswordPolicyService passwordPolicyService;
    @Mock AuthEmailNotificationService authEmailNotificationService;

    @InjectMocks AuthService authService;

    private static final String EMAIL = "alice@example.com";
    private static final String PASSWORD = "correct-password";
    private static final String HASH = "$2a$bcrypt-hash";
    private static final UUID USER_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(authService, "mfaRequiredRolesRaw", "");
    }

    // ── authenticate ──────────────────────────────────────────────────────

    @Test
    void authenticate_success_returnsTokens() {
        User user = activeUser(USER_ID, EMAIL, HASH, UserRole.CUSTOMER);
        stubLogin(user, true);

        AuthResponse response = authService.authenticate(req(EMAIL, PASSWORD, null), "127.0.0.1");

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUserId()).isEqualTo(USER_ID);
        verify(userRepository).save(user);
    }

    @Test
    void authenticate_emailNormalized_lowercase() {
        User user = activeUser(USER_ID, EMAIL, HASH, UserRole.CUSTOMER);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
        stubJwt(user);

        AuthResponse response = authService.authenticate(req("ALICE@EXAMPLE.COM", PASSWORD, null), "127.0.0.1");
        assertThat(response.getEmail()).isEqualTo(EMAIL);
    }

    @Test
    void authenticate_unknownEmail_throwsInvalidCredentials() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.authenticate(req(EMAIL, PASSWORD, null), "127.0.0.1"))
                .isInstanceOf(AuthService.AuthenticationException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void authenticate_inactiveAccount_throwsNotActive() {
        User user = userWithStatus(USER_ID, EMAIL, HASH, UserStatus.PENDING_VERIFICATION, UserRole.CUSTOMER);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.authenticate(req(EMAIL, PASSWORD, null), "127.0.0.1"))
                .isInstanceOf(AuthService.AuthenticationException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void authenticate_lockedAccount_throwsLocked() {
        User user = activeUser(USER_ID, EMAIL, HASH, UserRole.CUSTOMER);
        user.lockAccount(30);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.authenticate(req(EMAIL, PASSWORD, null), "127.0.0.1"))
                .isInstanceOf(AuthService.AuthenticationException.class)
                .hasMessageContaining("locked");
    }

    @Test
    void authenticate_wrongPassword_throwsInvalidCredentials() {
        User user = activeUser(USER_ID, EMAIL, HASH, UserRole.CUSTOMER);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(false);

        assertThatThrownBy(() -> authService.authenticate(req(EMAIL, PASSWORD, null), "127.0.0.1"))
                .isInstanceOf(AuthService.AuthenticationException.class)
                .hasMessageContaining("Invalid credentials");

        verify(userRepository).save(user);
    }

    @Test
    void authenticate_mfaEnabled_noCode_throwsMfaRequired() {
        User user = activeUser(USER_ID, EMAIL, HASH, UserRole.CUSTOMER);
        user.setMfaEnabled(true);
        user.setMfaSecretCipher("encrypted-secret");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);

        assertThatThrownBy(() -> authService.authenticate(req(EMAIL, PASSWORD, null), "127.0.0.1"))
                .isInstanceOf(AuthService.AuthenticationException.class)
                .hasMessage("MFA code required");
    }

    @Test
    void authenticate_mfaEnabled_invalidCode_throwsInvalidMfa() {
        User user = activeUser(USER_ID, EMAIL, HASH, UserRole.CUSTOMER);
        user.setMfaEnabled(true);
        user.setMfaSecretCipher("encrypted-secret");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
        when(piiCryptoService.decrypt("encrypted-secret")).thenReturn("plaintext-secret");
        when(totpService.verify("plaintext-secret", "000000")).thenReturn(false);

        assertThatThrownBy(() -> authService.authenticate(req(EMAIL, PASSWORD, "000000"), "127.0.0.1"))
                .isInstanceOf(AuthService.AuthenticationException.class)
                .hasMessage("Invalid MFA code");
    }

    @Test
    void authenticate_mfaEnabled_validCode_returnsTokens() {
        User user = activeUser(USER_ID, EMAIL, HASH, UserRole.CUSTOMER);
        user.setMfaEnabled(true);
        user.setMfaSecretCipher("encrypted-secret");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
        when(piiCryptoService.decrypt("encrypted-secret")).thenReturn("plaintext-secret");
        when(totpService.verify("plaintext-secret", "123456")).thenReturn(true);
        stubJwt(user);

        AuthResponse response = authService.authenticate(req(EMAIL, PASSWORD, "123456"), "127.0.0.1");
        assertThat(response.getAccessToken()).isEqualTo("access-token");
    }

    @Test
    void authenticate_mfaEnrollmentRequired_throwsMfaEnrollmentRequired() {
        ReflectionTestUtils.setField(authService, "mfaRequiredRolesRaw", "SUPER_ADMIN");
        User user = activeUser(USER_ID, EMAIL, HASH, UserRole.SUPER_ADMIN);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);

        assertThatThrownBy(() -> authService.authenticate(req(EMAIL, PASSWORD, null), "127.0.0.1"))
                .isInstanceOf(MfaEnrollmentRequiredException.class);
    }

    // ── refreshToken ─────────────────────────────────────────────────────

    @Test
    void refreshToken_invalidToken_throws() {
        when(jwtService.validateToken("bad-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken("bad-token"))
                .isInstanceOf(AuthService.AuthenticationException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void refreshToken_revokedToken_throws() {
        when(jwtService.validateToken("revoked")).thenReturn(true);
        when(jwtService.extractJti("revoked")).thenReturn("jti-123");
        when(jwtTokenBlocklistService.isRevoked("jti-123")).thenReturn(true);

        assertThatThrownBy(() -> authService.refreshToken("revoked"))
                .isInstanceOf(AuthService.AuthenticationException.class)
                .hasMessageContaining("no longer valid");
    }

    @Test
    void refreshToken_userNotFound_throws() {
        String token = "valid-token";
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractJti(token)).thenReturn("jti-1");
        when(jwtTokenBlocklistService.isRevoked("jti-1")).thenReturn(false);
        when(jwtService.extractUserId(token)).thenReturn(USER_ID.toString());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(token))
                .isInstanceOf(AuthService.AuthenticationException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void refreshToken_success_returnsNewTokens() {
        User user = activeUser(USER_ID, EMAIL, HASH, UserRole.CUSTOMER);
        String token = "valid-refresh";
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractJti(token)).thenReturn("jti-1");
        when(jwtTokenBlocklistService.isRevoked("jti-1")).thenReturn(false);
        when(jwtService.extractUserId(token)).thenReturn(USER_ID.toString());
        when(jwtService.extractTokenVersion(token)).thenReturn(0);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(jwtService.extractExpirationInstant(token)).thenReturn(Instant.now().plusSeconds(3600));
        stubJwt(user);

        AuthResponse response = authService.refreshToken(token);
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    // ── forgotPassword ───────────────────────────────────────────────────

    @Test
    void forgotPassword_unknownEmail_silentNoOp() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("unknown@example.com");

        authService.forgotPassword(req);

        verifyNoInteractions(passwordResetTokenRepository);
        verifyNoInteractions(authEmailNotificationService);
    }

    @Test
    void forgotPassword_knownEmail_savesTokenAndSendsEmail() {
        User user = activeUser(USER_ID, EMAIL, HASH, UserRole.CUSTOMER);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail(EMAIL);

        authService.forgotPassword(req);

        verify(passwordResetTokenRepository).deleteByUserId(USER_ID);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(authEmailNotificationService).sendPasswordReset(eq(EMAIL), any(String.class));
    }

    // ── resetPassword ────────────────────────────────────────────────────

    @Test
    void resetPassword_invalidToken_throws() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("bad-token");
        req.setNewPassword("NewP@ss123!");
        when(passwordResetTokenRepository.findValidByToken(eq("bad-token"), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(AuthService.AuthenticationException.class)
                .hasMessageContaining("Invalid or expired");
    }

    @Test
    void resetPassword_validToken_updatesPasswordAndDeletesToken() {
        User user = activeUser(USER_ID, EMAIL, HASH, UserRole.CUSTOMER);
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(USER_ID);
        token.setToken("valid-token");

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("valid-token");
        req.setNewPassword("NewP@ss123!");

        when(passwordResetTokenRepository.findValidByToken(eq("valid-token"), any())).thenReturn(Optional.of(token));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewP@ss123!")).thenReturn("new-hash");

        authService.resetPassword(req);

        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).deleteByUserId(USER_ID);
        verify(passwordHistoryService).recordPasswordRotation(eq(USER_ID), eq(HASH));
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private static User activeUser(UUID id, String email, String hash, UserRole role) {
        return userWithStatus(id, email, hash, UserStatus.ACTIVE, role);
    }

    private static User userWithStatus(UUID id, String email, String hash, UserStatus status, UserRole role) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setPasswordHash(hash);
        u.setStatus(status);
        u.setRole(role);
        return u;
    }

    private void stubLogin(User user, boolean passwordMatches) {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(passwordMatches);
        if (passwordMatches) {
            stubJwt(user);
        }
    }

    private void stubJwt(User user) {
        when(serviceProviderRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(providerStaffRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(jwtService.generateAccessToken(eq(user), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(jwtService.getExpirationTime()).thenReturn(3600L);
    }

    private static AuthRequest req(String email, String password, String mfaCode) {
        AuthRequest r = new AuthRequest();
        r.setEmail(email);
        r.setPassword(password);
        r.setMfaCode(mfaCode);
        return r;
    }
}
