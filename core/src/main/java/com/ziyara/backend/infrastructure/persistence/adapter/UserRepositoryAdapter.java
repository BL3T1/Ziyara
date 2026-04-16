package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.enums.UserStatus;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.infrastructure.persistence.entity.UserJpaEntity;
import com.ziyara.backend.infrastructure.persistence.mapper.UserMapper;
import com.ziyara.backend.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository Adapter: UserRepositoryAdapter
 * Implements domain repository interface using JPA
 * Part of Clean Architecture - Infrastructure Layer
 */
@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {
    
    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;
    
    @Override
    public User save(User user) {
        UserJpaEntity entity = userMapper.toJpaEntity(user);
        UserJpaEntity savedEntity = userJpaRepository.save(entity);
        return userMapper.toDomainEntity(savedEntity);
    }
    
    @Override
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id)
                .map(userMapper::toDomainEntity);
    }

    @Override
    public List<User> findAllById(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return userJpaRepository.findAllById(ids).stream()
                .map(userMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        String trimmed = email.trim();
        return userJpaRepository.findByEmail(trimmed)
                .or(() -> userJpaRepository.findByEmailIgnoreCase(trimmed))
                .map(userMapper::toDomainEntity);
    }
    
    @Override
    public Optional<User> findByPhone(String phone) {
        return userJpaRepository.findByPhone(phone)
                .map(userMapper::toDomainEntity);
    }
    
    @Override
    public void deleteById(UUID id) {
        userJpaRepository.deleteById(id);
    }
    
    @Override
    public void delete(User user) {
        UserJpaEntity entity = userMapper.toJpaEntity(user);
        userJpaRepository.delete(entity);
    }
    
    @Override
    public List<User> findAll() {
        return userJpaRepository.findAll().stream()
                .map(userMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<User> findByStatus(UserStatus status) {
        return userJpaRepository.findByStatus(status).stream()
                .map(userMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<User> findByRole(UserRole role) {
        return userJpaRepository.findByRole(role).stream()
                .map(userMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<User> findByEmailContaining(String emailPattern) {
        return userJpaRepository.findByEmailContaining(emailPattern).stream()
                .map(userMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String t = email.trim();
        return userJpaRepository.existsByEmail(t) || userJpaRepository.existsByEmailIgnoreCase(t);
    }
    
    @Override
    public boolean existsByPhone(String phone) {
        return userJpaRepository.existsByPhone(phone);
    }
    
    @Override
    public boolean existsById(UUID id) {
        return userJpaRepository.existsById(id);
    }
    
    @Override
    public Optional<User> findByEmailAndStatus(String email, UserStatus status) {
        return userJpaRepository.findByEmailAndStatus(email, status)
                .map(userMapper::toDomainEntity);
    }
    
    @Override
    public Optional<User> findByPhoneAndStatus(String phone, UserStatus status) {
        return userJpaRepository.findByPhoneAndStatus(phone, status)
                .map(userMapper::toDomainEntity);
    }
    
    @Override
    public long count() {
        return userJpaRepository.count();
    }
    
    @Override
    public long countByStatus(UserStatus status) {
        return userJpaRepository.countByStatus(status);
    }
    
    @Override
    public long countByRole(UserRole role) {
        return userJpaRepository.countByRole(role);
    }
}
