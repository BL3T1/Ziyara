package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.UserPasswordHistoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserPasswordHistoryJpaRepository extends JpaRepository<UserPasswordHistoryJpaEntity, UUID> {

    @Query("select h.passwordHash from UserPasswordHistoryJpaEntity h where h.userId = :userId order by h.createdAt desc")
    List<String> findPasswordHashesByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    @Query("select h.id from UserPasswordHistoryJpaEntity h where h.userId = :userId order by h.createdAt asc")
    List<UUID> findIdsByUserIdOrderByCreatedAtAsc(@Param("userId") UUID userId);
}
