package com.ziyara.backend.application.service;

import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.enums.UserStatus;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.infrastructure.security.crypto.PiiCryptoService;
import com.ziyara.backend.infrastructure.security.crypto.TotpService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserMfaServiceTest {

    @Mock UserRepository userRepository;
    @Mock TotpService totpService;
    @Mock PiiCryptoService piiCryptoService;

    @InjectMocks UserMfaService mfaService;

    private static final UUID USER_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    // ── startEnrollment ───────────────────────────────────────────────────────

    @Test
    void startEnrollment_userNotFound_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mfaService.startEnrollment(USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void startEnrollment_alreadyEnabled_throws() {
        User user = activeUser();
        user.setMfaEnabled(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> mfaService.startEnrollment(USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already enabled");
    }

    @Test
    void startEnrollment_success_savesEncryptedSecretAndReturnsUri() {
        User user = activeUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(totpService.generateSecret()).thenReturn("JBSWY3DPEHPK3PXP");
        when(piiCryptoService.encrypt("JBSWY3DPEHPK3PXP")).thenReturn("cipher");
        when(totpService.toOtpAuthUri("Ziyara", user.getEmail(), "JBSWY3DPEHPK3PXP"))
                .thenReturn("otpauth://totp/Ziyara:alice@example.com?secret=JBSWY3DPEHPK3PXP");

        var result = mfaService.startEnrollment(USER_ID);

        assertThat(result.base32Secret()).isEqualTo("JBSWY3DPEHPK3PXP");
        assertThat(result.otpauthUri()).contains("JBSWY3DPEHPK3PXP");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getMfaSecretCipher()).isEqualTo("cipher");
        assertThat(captor.getValue().isMfaEnabled()).isFalse();
    }

    // ── confirmEnrollment ─────────────────────────────────────────────────────

    @Test
    void confirmEnrollment_noSecretStored_throws() {
        User user = activeUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> mfaService.confirmEnrollment(USER_ID, "123456"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Start MFA enrollment first");
    }

    @Test
    void confirmEnrollment_invalidCode_throws() {
        User user = activeUser();
        user.setMfaSecretCipher("cipher");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(piiCryptoService.decrypt("cipher")).thenReturn("plaintext-secret");
        when(totpService.verify("plaintext-secret", "000000")).thenReturn(false);

        assertThatThrownBy(() -> mfaService.confirmEnrollment(USER_ID, "000000"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid authenticator code");

        verify(userRepository, never()).save(any());
    }

    @Test
    void confirmEnrollment_validCode_enablesMfa() {
        User user = activeUser();
        user.setMfaSecretCipher("cipher");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(piiCryptoService.decrypt("cipher")).thenReturn("plaintext-secret");
        when(totpService.verify("plaintext-secret", "123456")).thenReturn(true);

        mfaService.confirmEnrollment(USER_ID, "123456");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().isMfaEnabled()).isTrue();
        assertThat(captor.getValue().getMfaEnrolledAt()).isNotNull();
    }

    // ── disable ───────────────────────────────────────────────────────────────

    @Test
    void disable_clearsMfaFieldsAndBumpsTokenVersion() {
        User user = activeUser();
        user.setMfaEnabled(true);
        user.setMfaSecretCipher("cipher");
        user.setMfaType("TOTP");
        int originalVersion = user.getTokenVersion();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        mfaService.disable(USER_ID);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.isMfaEnabled()).isFalse();
        assertThat(saved.getMfaSecretCipher()).isNull();
        assertThat(saved.getMfaBackupCodesCipher()).isNull();
        assertThat(saved.getMfaType()).isNull();
        assertThat(saved.getTokenVersion()).isEqualTo(originalVersion + 1);
    }

    private static User activeUser() {
        User u = new User();
        u.setId(USER_ID);
        u.setEmail("alice@example.com");
        u.setPasswordHash("$2a$hash");
        u.setRole(UserRole.CUSTOMER);
        u.setStatus(UserStatus.ACTIVE);
        return u;
    }
}
