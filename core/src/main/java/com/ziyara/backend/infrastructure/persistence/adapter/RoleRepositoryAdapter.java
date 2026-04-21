package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.Role;
import com.ziyara.backend.domain.repository.RoleRepository;
import com.ziyara.backend.infrastructure.persistence.entity.RoleJpaEntity;
import com.ziyara.backend.infrastructure.persistence.mapper.RoleMapper;
import com.ziyara.backend.infrastructure.persistence.repository.RoleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepository {

    private final RoleJpaRepository jpaRepository;
    private final RoleMapper mapper;

    @Override
    public Role save(Role role) {
        RoleJpaEntity entity = mapper.toJpaEntity(role);
        RoleJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomainEntity(saved);
    }

    @Override
    public Optional<Role> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public Optional<Role> findByCode(String code) {
        return jpaRepository.findByCode(code).map(mapper::toDomainEntity);
    }

    @Override
    public List<Role> findAllOrderByName() {
        return jpaRepository.findAllByOrderByNameAsc().stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Role> findByGroupIdOrderByName(UUID groupId) {
        return jpaRepository.findByGroupIdOrderByNameAsc(groupId).stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Role> findByGroupIdIsNullOrderByName() {
        return jpaRepository.findByGroupIdIsNullOrderByNameAsc().stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
