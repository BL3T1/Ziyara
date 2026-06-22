package com.ziyara.backend.domain.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface RolePermissionRepository {
    List<UUID> findPermissionIdsByRoleId(UUID roleId);
    Map<UUID, Set<UUID>> findPermissionIdsByRoleIds(Set<UUID> roleIds);
    void setPermissionsForRole(UUID roleId, List<UUID> permissionIds);
    void deleteByRoleId(UUID roleId);
}
