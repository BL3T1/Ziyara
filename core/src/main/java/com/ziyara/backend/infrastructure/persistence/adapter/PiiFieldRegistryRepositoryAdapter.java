package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.PiiFieldRegistry;
import com.ziyara.backend.domain.repository.PiiFieldRegistryRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.PiiFieldRegistryMapper;
import com.ziyara.backend.infrastructure.persistence.repository.PiiFieldRegistryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PiiFieldRegistryRepositoryAdapter implements PiiFieldRegistryRepository {

    private final PiiFieldRegistryJpaRepository jpaRepository;
    private final PiiFieldRegistryMapper mapper;

    @Override
    public PiiFieldRegistry save(PiiFieldRegistry entry) {
        return mapper.toDomainEntity(jpaRepository.save(mapper.toJpaEntity(entry)));
    }

    @Override
    public Optional<PiiFieldRegistry> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public List<PiiFieldRegistry> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
