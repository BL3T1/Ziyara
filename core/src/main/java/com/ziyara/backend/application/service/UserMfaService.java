package com.ziyara.backend.application.service;

import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.infrastructure.security.crypto.PiiCryptoService;
import com.ziyara.backend.infrastructure.security.crypto.TotpService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TOTP enrollment and disable flows (secrets stored ciphertext via {@link PiiCryptoService} when configured).
 */
@Service
@RequiredArgsConstructor
public class UserMfaService {

    private static final String ISSUER = "Ziyara";

    private final UserRepository userRepository;
    private final TotpService totpService;
    private final PiiCryptoService piiCryptoService;

    /**
     * Generates a new secret and persists it (MFA still disabled until {@link #confirmEnrollment(UUID, String)}).
     */
    @Transactional
    public MfaEnrollmentStartResult startEnrollment(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.isMfaEnabled()) {
            throw new IllegalStateException("MFA is already enabled");
        }
        String secret = totpService.generateSecret();
        user.setMfaType("TOTP");
        user.setMfaSecretCipher(piiCryptoService.encrypt(secret));
        user.setMfaEnabled(false);
        userRepository.save(user);
        String otpauth = totpService.toOtpAuthUri(ISSUER, user.getEmail(), secret);
        return new MfaEnrollmentStartResult(secret, otpauth);
    }

    @Transactional
    public void confirmEnrollment(UUID userId, String code) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        String cipher = user.getMfaSecretCipher();
        if (cipher == null || cipher.isBlank()) {
            throw new IllegalStateException("Start MFA enrollment first");
        }
        String secret = piiCryptoService.decrypt(cipher);
        if (!totpService.verify(secret, code.trim())) {
            throw new IllegalArgumentException("Invalid authenticator code");
        }
        user.setMfaEnabled(true);
        user.setMfaEnrolledAt(LocalDateTime.now());
        user.setMfaType("TOTP");
        userRepository.save(user);
    }

    @Transactional
    public void disable(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setMfaEnabled(false);
        user.setMfaSecretCipher(null);
        user.setMfaBackupCodesCipher(null);
        user.setMfaType(null);
        user.setMfaEnrolledAt(null);
        user.incrementTokenVersion();
        userRepository.save(user);
    }

    public record MfaEnrollmentStartResult(String base32Secret, String otpauthUri) {}
}
