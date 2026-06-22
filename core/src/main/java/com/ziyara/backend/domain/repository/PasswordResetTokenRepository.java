package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.PasswordResetToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository {

    PasswordResetToken save(PasswordResetToken token);

    Optional<PasswordResetToken> findValidByToken(String token, Instant now);

    void deleteByUserId(UUID userId);

    void deleteExpiredBefore(Instant cutoff);
}
