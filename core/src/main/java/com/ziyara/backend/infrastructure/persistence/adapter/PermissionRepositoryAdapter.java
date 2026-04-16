package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.Permission;
import com.ziyara.backend.domain.repository.PermissionRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.PermissionMapper;
import com.ziyara.backend.infrastructure.persistence.repository.PermissionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PermissionRepositoryAdapter implements PermissionRepository {

    private final PermissionJpaRepository jpaRepository;
    private final PermissionMapper mapper;

    @Override
    public List<Permission> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomainEntity).collect(Collectors.toList());
    }

    @Override
    public List<Permission> findAllUnlocked() {
        return jpaRepository.findByIsLockedFalse().stream().map(mapper::toDomainEntity).collect(Collectors.toList());
    }

    @Override
    public Optional<Permission> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }
}
