package com.ziyarah.infrastructure.persistence.adapter;

import com.ziyarah.domain.entity.User;
import com.ziyarah.domain.enums.UserRole;
import com.ziyarah.domain.enums.UserStatus;
import com.ziyarah.domain.repository.UserRepository;
import com.ziyarah.infrastructure.persistence.entity.UserJpaEntity;
import com.ziyarah.infrastructure.persistence.mapper.UserMapper;
import com.ziyarah.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email)
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
        return userJpaRepository.existsByEmail(email);
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
