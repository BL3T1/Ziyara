package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.enums.UserStatus;
import com.ziyara.backend.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository: UserJpaRepository
 * Infrastructure layer implementation for User data access
 */
@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {
    
    Optional<UserJpaEntity> findByEmail(String email);

    Optional<UserJpaEntity> findByEmailIgnoreCase(String email);

    Optional<UserJpaEntity> findByUsername(String username);

    boolean existsByUsername(String username);
    
    Optional<UserJpaEntity> findByPhone(String phone);
    
    List<UserJpaEntity> findByStatus(UserStatus status);
    
    List<UserJpaEntity> findByRole(UserRole role);
    
    List<UserJpaEntity> findByEmailContaining(String emailPattern);
    
    Optional<UserJpaEntity> findByEmailAndStatus(String email, UserStatus status);
    
    Optional<UserJpaEntity> findByPhoneAndStatus(String phone, UserStatus status);
    
    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);
    
    boolean existsByPhone(String phone);
    
    long countByStatus(UserStatus status);
    
    long countByRole(UserRole role);
    
    @Query("select distinct u.id from UserJpaEntity u where u.role in :roles and u.status = :status")
    List<UUID> findDistinctIdsByRoleInAndStatus(@Param("roles") Collection<UserRole> roles,
                                                @Param("status") UserStatus status);
}
