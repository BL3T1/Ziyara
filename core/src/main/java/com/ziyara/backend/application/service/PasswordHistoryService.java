package com.ziyara.backend.application.service;

import com.ziyara.backend.infrastructure.persistence.entity.UserPasswordHistoryJpaEntity;
import com.ziyara.backend.infrastructure.persistence.repository.UserPasswordHistoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Stores prior password hashes and prevents reuse of recent passwords.
 */
@Service
@RequiredArgsConstructor
public class PasswordHistoryService {

    private static final int MAX_HISTORY = 12;

    private final UserPasswordHistoryJpaRepository repository;

    /**
     * Ensures the new password is not the same as the current hash and not equal to any of the last stored hashes.
     */
    public void assertPasswordNotReused(UUID userId, String newPlainPassword, PasswordEncoder encoder, String currentPasswordHash) {
        if (encoder.matches(newPlainPassword, currentPasswordHash)) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }
        List<String> previous = repository.findPasswordHashesByUserIdOrderByCreatedAtDesc(userId);
        for (String hash : previous) {
            if (encoder.matches(newPlainPassword, hash)) {
                throw new IllegalArgumentException("Cannot reuse a recent password");
            }
        }
    }

    @Transactional
    public void recordPasswordRotation(UUID userId, String previousPasswordHash) {
        repository.save(UserPasswordHistoryJpaEntity.builder()
                .userId(userId)
                .passwordHash(previousPasswordHash)
                .build());
        trimOldest(userId);
    }

    private void trimOldest(UUID userId) {
        List<UUID> ids = repository.findIdsByUserIdOrderByCreatedAtAsc(userId);
        if (ids.size() <= MAX_HISTORY) {
            return;
        }
        int excess = ids.size() - MAX_HISTORY;
        repository.deleteAllById(ids.subList(0, excess));
    }
}
