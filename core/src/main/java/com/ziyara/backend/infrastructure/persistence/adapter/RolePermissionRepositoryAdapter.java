package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.repository.RolePermissionRepository;
import com.ziyara.backend.infrastructure.persistence.entity.RolePermissionJpaEntity;
import com.ziyara.backend.infrastructure.persistence.repository.RolePermissionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RolePermissionRepositoryAdapter implements RolePermissionRepository {

    private final RolePermissionJpaRepository jpaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findPermissionIdsByRoleId(UUID roleId) {
        return jpaRepository.findByRoleId(roleId).stream()
                .map(RolePermissionJpaEntity::getPermissionId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void setPermissionsForRole(UUID roleId, List<UUID> permissionIds) {
        jpaRepository.deleteByRoleId(roleId);
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (UUID permissionId : permissionIds) {
                jpaRepository.save(RolePermissionJpaEntity.builder()
                        .roleId(roleId)
                        .permissionId(permissionId)
                        .build());
            }
        }
    }

    @Override
    @Transactional
    public void deleteByRoleId(UUID roleId) {
        jpaRepository.deleteByRoleId(roleId);
    }
}
