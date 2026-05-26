package com.ziyara.backend.domain.repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Count and reassign user-role assignments ({@code sys_user_roles}).
 */
public interface UserRoleAssignmentRepository {
    long countByRoleId(UUID roleId);

    long countByGroupId(UUID groupId);

    void reassignAllToRole(UUID fromRoleId, UUID targetRoleId);

    /** Newest assignment first (by {@code assigned_at}, then id). Empty if none. */
    Optional<UUID> findNewestRoleIdForUser(UUID userId);

    /** v1: at most one row per user — replaces any existing assignments. */
    void setPrimaryRoleForUser(UUID userId, UUID roleId);

    void clearAssignmentsForUser(UUID userId);

    /** Returns all permission codes granted to the user via their assigned roles. */
    java.util.List<String> findPermissionCodesByUserId(UUID userId);
}
