package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.UserRoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserRoleJpaRepository extends JpaRepository<UserRoleJpaEntity, UUID> {
    long countByRoleId(UUID roleId);

    @Query("SELECT ur.roleId, COUNT(ur) FROM UserRoleJpaEntity ur WHERE ur.roleId IN :roleIds GROUP BY ur.roleId")
    List<Object[]> countByRoleIdIn(@Param("roleIds") Collection<UUID> roleIds);

    long countByGroupId(UUID groupId);

    List<UserRoleJpaEntity> findByRoleId(UUID roleId);

    @Query("SELECT ur FROM UserRoleJpaEntity ur WHERE ur.userId = :userId ORDER BY ur.assignedAt DESC NULLS LAST, ur.id DESC")
    List<UserRoleJpaEntity> findByUserIdOrderNewestFirst(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM UserRoleJpaEntity ur WHERE ur.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE UserRoleJpaEntity ur SET ur.roleId = :targetRoleId WHERE ur.roleId = :fromRoleId")
    int reassignAllToRole(@Param("fromRoleId") UUID fromRoleId, @Param("targetRoleId") UUID targetRoleId);

    @Modifying
    @Query("UPDATE UserRoleJpaEntity ur SET ur.groupId = null WHERE ur.groupId = :groupId")
    int clearGroupId(@Param("groupId") UUID groupId);

    @Query(value = "SELECT DISTINCT p.code FROM sys_user_roles ur " +
                   "JOIN sys_role_permissions rp ON rp.role_id = ur.role_id " +
                   "JOIN sys_permissions p ON p.id = rp.permission_id " +
                   "WHERE ur.user_id = :userId",
           nativeQuery = true)
    List<String> findPermissionCodesByUserId(@Param("userId") UUID userId);

    @Query(value = "SELECT DISTINCT p.code FROM sys_roles r " +
                   "JOIN sys_role_permissions rp ON rp.role_id = r.id " +
                   "JOIN sys_permissions p ON p.id = rp.permission_id " +
                   "WHERE r.code = :roleCode AND r.is_system_role = TRUE",
           nativeQuery = true)
    List<String> findPermissionCodesBySystemRoleCode(@Param("roleCode") String roleCode);
}
