package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.PasswordResetToken;
import com.ziyara.backend.domain.repository.PasswordResetTokenRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.PasswordResetTokenMapper;
import com.ziyara.backend.infrastructure.persistence.repository.PasswordResetTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository jpaRepository;
    private final PasswordResetTokenMapper mapper;

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        return mapper.toDomainEntity(jpaRepository.save(mapper.toJpaEntity(token)));
    }

    @Override
    public Optional<PasswordResetToken> findValidByToken(String token, Instant now) {
        return jpaRepository.findByTokenAndExpiresAtAfter(token, now)
                .map(mapper::toDomainEntity);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        jpaRepository.deleteByUserId(userId);
    }

    @Override
    public void deleteExpiredBefore(Instant cutoff) {
        jpaRepository.deleteByExpiresAtBefore(cutoff);
    }
}
