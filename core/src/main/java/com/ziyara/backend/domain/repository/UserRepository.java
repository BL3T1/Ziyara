package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.enums.UserStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository Port: UserRepository
 * Interface for user data access - defined in domain layer
 * Implemented by infrastructure layer (Dependency Inversion)
 */
public interface UserRepository {
    
    // CRUD Operations
    User save(User user);
    Optional<User> findById(UUID id);

    /** Batch load by id (avoids N+1 when resolving many users). */
    List<User> findAllById(Collection<UUID> ids);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    void deleteById(UUID id);
    void delete(User user);
    
    // Query Operations
    List<User> findAll();
    List<User> findByStatus(UserStatus status);
    List<User> findByRole(UserRole role);
    List<User> findByEmailContaining(String emailPattern);
    
    // Existence checks
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsById(UUID id);
    
    // Authentication support
    Optional<User> findByEmailAndStatus(String email, UserStatus status);
    Optional<User> findByPhoneAndStatus(String phone, UserStatus status);
    
    // Pagination support
    long count();
    long countByStatus(UserStatus status);
    long countByRole(UserRole role);

    /** Active (non-deleted) users with any of the given roles and status. */
    List<UUID> findActiveDirectoryUserIdsByRoles(Collection<UserRole> roles, UserStatus status);
}
