package com.ziyarah.domain.repository;

import com.ziyarah.domain.entity.User;
import com.ziyarah.domain.enums.UserRole;
import com.ziyarah.domain.enums.UserStatus;
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
}
