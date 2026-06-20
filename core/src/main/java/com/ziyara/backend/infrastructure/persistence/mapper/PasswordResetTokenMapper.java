package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.PasswordResetToken;
import com.ziyara.backend.infrastructure.persistence.entity.PasswordResetTokenJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetTokenMapper {

    public PasswordResetToken toDomainEntity(PasswordResetTokenJpaEntity entity) {
        if (entity == null) return null;
        PasswordResetToken token = new PasswordResetToken();
        token.setId(entity.getId());
        token.setUserId(entity.getUserId());
        token.setToken(entity.getToken());
        token.setExpiresAt(entity.getExpiresAt());
        token.setCreatedAt(entity.getCreatedAt());
        return token;
    }

    public PasswordResetTokenJpaEntity toJpaEntity(PasswordResetToken token) {
        if (token == null) return null;
        return PasswordResetTokenJpaEntity.builder()
                .id(token.getId())
                .userId(token.getUserId())
                .token(token.getToken())
                .expiresAt(token.getExpiresAt())
                .createdAt(token.getCreatedAt())
                .build();
    }
}
