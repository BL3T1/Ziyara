package com.ziyarah.infrastructure.persistence.repository;

import com.ziyarah.domain.enums.UserRole;
import com.ziyarah.domain.enums.UserStatus;
import com.ziyarah.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
    
    Optional<UserJpaEntity> findByPhone(String phone);
    
    List<UserJpaEntity> findByStatus(UserStatus status);
    
    List<UserJpaEntity> findByRole(UserRole role);
    
    List<UserJpaEntity> findByEmailContaining(String emailPattern);
    
    Optional<UserJpaEntity> findByEmailAndStatus(String email, UserStatus status);
    
    Optional<UserJpaEntity> findByPhoneAndStatus(String phone, UserStatus status);
    
    boolean existsByEmail(String email);
    
    boolean existsByPhone(String phone);
    
    long countByStatus(UserStatus status);
    
    long countByRole(UserRole role);
    
    @Query("SELECT u FROM UserJpaEntity u WHERE u.deletedAt IS NULL")
    List<UserJpaEntity> findAllActive();
}
