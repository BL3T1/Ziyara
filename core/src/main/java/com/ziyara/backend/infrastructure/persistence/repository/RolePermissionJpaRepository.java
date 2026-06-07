package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.RolePermissionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface RolePermissionJpaRepository extends JpaRepository<RolePermissionJpaEntity, UUID> {
    List<RolePermissionJpaEntity> findByRoleId(UUID roleId);

    @Modifying
    @Query("DELETE FROM RolePermissionJpaEntity r WHERE r.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") UUID roleId);
}
