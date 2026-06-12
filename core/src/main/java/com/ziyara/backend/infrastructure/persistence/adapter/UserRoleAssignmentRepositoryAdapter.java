package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.repository.UserRoleAssignmentRepository;
import com.ziyara.backend.infrastructure.persistence.entity.UserRoleJpaEntity;
import com.ziyara.backend.infrastructure.persistence.repository.RoleJpaRepository;
import com.ziyara.backend.infrastructure.persistence.repository.UserRoleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public Map<UUID, Long> countByRoleIdIn(Set<UUID> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return Map.of();
        return jpaRepository.countByRoleIdIn(roleIds).stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
    }

    @Override
    public long countByGroupId(UUID groupId) {
        return jpaRepository.countByGroupId(groupId);
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "userPermissions", allEntries = true)
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
    @Cacheable(value = "userPermissions", key = "#userId", unless = "#result == null || #result.isEmpty()")
    public List<String> findPermissionCodesByUserId(UUID userId) {
        return jpaRepository.findPermissionCodesByUserId(userId);
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.Caching(evict = {
            @CacheEvict(value = "userPermissions", key = "#userId"),
            @CacheEvict(value = "userDetails", key = "#userId.toString()")
    })
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
    @CacheEvict(value = "userPermissions", key = "#userId")
    public void clearAssignmentsForUser(UUID userId) {
        jpaRepository.deleteByUserId(userId);
    }

    @Override
    public List<UUID> findUserIdsByRoleId(UUID roleId) {
        return jpaRepository.findByRoleId(roleId).stream()
                .map(UserRoleJpaEntity::getUserId)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<String> findPermissionCodesBySystemRoleCode(String roleCode) {
        return jpaRepository.findPermissionCodesBySystemRoleCode(roleCode);
    }

    @Override
    @Transactional
    @CacheEvict(value = "userPermissions", allEntries = true)
    public void clearGroupId(UUID groupId) {
        jpaRepository.clearGroupId(groupId);
    }
}
