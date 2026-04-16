package com.ziyara.backend.domain.repository;

import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository {
    List<UUID> findPermissionIdsByRoleId(UUID roleId);
    void setPermissionsForRole(UUID roleId, List<UUID> permissionIds);
    void deleteByRoleId(UUID roleId);
}
