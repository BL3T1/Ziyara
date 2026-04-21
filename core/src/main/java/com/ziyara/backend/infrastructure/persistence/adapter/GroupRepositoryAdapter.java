package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.Group;
import com.ziyara.backend.domain.repository.GroupRepository;
import com.ziyara.backend.infrastructure.persistence.entity.GroupJpaEntity;
import com.ziyara.backend.infrastructure.persistence.mapper.GroupMapper;
import com.ziyara.backend.infrastructure.persistence.repository.GroupJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GroupRepositoryAdapter implements GroupRepository {

    private final GroupJpaRepository jpaRepository;
    private final GroupMapper mapper;

    @Override
    public List<Group> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomainEntity).collect(Collectors.toList());
    }

    @Override
    public Optional<Group> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public Group save(Group group) {
        GroupJpaEntity entity = mapper.toJpaEntity(group);
        GroupJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomainEntity(saved);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }

    @Override
    public boolean existsByCodeAndIdNot(String code, UUID excludeId) {
        return jpaRepository.existsByCodeAndIdNot(code, excludeId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
