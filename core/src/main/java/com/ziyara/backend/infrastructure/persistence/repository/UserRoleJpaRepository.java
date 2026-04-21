package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.UserRoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserRoleJpaRepository extends JpaRepository<UserRoleJpaEntity, UUID> {
    long countByRoleId(UUID roleId);

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
}
