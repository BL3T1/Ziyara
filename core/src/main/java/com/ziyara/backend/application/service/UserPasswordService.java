package com.ziyara.backend.application.service;

import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Owns the password-reset operation so both {@link UserCommandHandler} and
 * {@link ServiceProviderService} can call it without creating a command→service cycle.
 */
@Service
@RequiredArgsConstructor
public class UserPasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordHistoryService passwordHistoryService;
    private final PasswordPolicyService passwordPolicyService;

    @Transactional
    public void resetPassword(UUID userId, String newPassword) {
        passwordPolicyService.assertAcceptable(newPassword);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        passwordHistoryService.assertPasswordNotReused(userId, newPassword, passwordEncoder, user.getPasswordHash());
        passwordHistoryService.recordPasswordRotation(userId, user.getPasswordHash());
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setLastPasswordChange(LocalDateTime.now());
        user.setMustChangePassword(true);
        // Do NOT increment token version here — the user's existing session must stay
        // valid so they can reach the change-password endpoint. Token version is
        // incremented in UserCommandHandler.changePassword() once they successfully set
        // their new password, which then invalidates all other sessions.
        userRepository.save(user);
    }
}
