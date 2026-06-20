package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.repository.UserPasswordHistoryRepository;
import com.ziyara.backend.infrastructure.persistence.entity.UserPasswordHistoryJpaEntity;
import com.ziyara.backend.infrastructure.persistence.repository.UserPasswordHistoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserPasswordHistoryRepositoryAdapter implements UserPasswordHistoryRepository {

    private final UserPasswordHistoryJpaRepository jpaRepository;

    @Override
    public void save(UUID userId, String passwordHash) {
        UserPasswordHistoryJpaEntity entity = UserPasswordHistoryJpaEntity.builder()
                .userId(userId)
                .passwordHash(passwordHash)
                .build();
        jpaRepository.save(entity);
    }

    @Override
    public List<String> findHashesByUserId(UUID userId) {
        return jpaRepository.findPasswordHashesByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<UUID> findIdsByUserIdOldestFirst(UUID userId) {
        return jpaRepository.findIdsByUserIdOrderByCreatedAtAsc(userId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
