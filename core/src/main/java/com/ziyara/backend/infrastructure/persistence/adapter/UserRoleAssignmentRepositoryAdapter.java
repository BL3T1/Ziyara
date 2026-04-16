package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.repository.UserRoleAssignmentRepository;
import com.ziyara.backend.infrastructure.persistence.entity.UserRoleJpaEntity;
import com.ziyara.backend.infrastructure.persistence.repository.RoleJpaRepository;
import com.ziyara.backend.infrastructure.persistence.repository.UserRoleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserRoleAssignmentRepositoryAdapter implements UserRoleAssignmentRepository {

    private final UserRoleJpaRepository jpaRepository;
    private final RoleJpaRepository roleJpaRepository;

    @Override
    public long countByRoleId(UUID roleId) {
        return jpaRepository.countByRoleId(roleId);
    }

    @Override
    @Transactional
    public void reassignAllToRole(UUID fromRoleId, UUID targetRoleId) {
        jpaRepository.reassignAllToRole(fromRoleId, targetRoleId);
    }

    @Override
    public Optional<UUID> findNewestRoleIdForUser(UUID userId) {
        List<UserRoleJpaEntity> rows = jpaRepository.findByUserIdOrderNewestFirst(userId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0).getRoleId());
    }

    @Override
    @Transactional
    public void setPrimaryRoleForUser(UUID userId, UUID roleId) {
        jpaRepository.deleteByUserId(userId);
        UserRoleJpaEntity row = UserRoleJpaEntity.builder()
                .userId(userId)
                .roleId(roleId)
                .groupId(roleJpaRepository.findById(roleId).map(r -> r.getGroupId()).orElse(null))
                .assignedAt(LocalDateTime.now())
                .build();
        jpaRepository.save(row);
    }

    @Override
    @Transactional
    public void clearAssignmentsForUser(UUID userId) {
        jpaRepository.deleteByUserId(userId);
    }
}
