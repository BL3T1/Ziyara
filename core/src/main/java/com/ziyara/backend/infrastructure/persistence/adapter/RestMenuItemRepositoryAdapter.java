package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.RestMenuItem;
import com.ziyara.backend.domain.repository.RestMenuItemRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.RestMenuItemMapper;
import com.ziyara.backend.infrastructure.persistence.repository.RestMenuItemJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RestMenuItemRepositoryAdapter implements RestMenuItemRepository {

    private final RestMenuItemJpaRepository jpaRepository;
    private final RestMenuItemMapper mapper;

    @Override
    public RestMenuItem save(RestMenuItem item) {
        return mapper.toDomainEntity(jpaRepository.save(mapper.toJpaEntity(item)));
    }

    @Override
    public Optional<RestMenuItem> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public List<RestMenuItem> findBySectionId(UUID sectionId) {
        return jpaRepository.findBySectionIdOrderBySortOrderAscIdAsc(sectionId).stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<RestMenuItem> findAllById(List<UUID> ids) {
        return jpaRepository.findAllById(ids).stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long countBySectionId(UUID sectionId) {
        return jpaRepository.countBySectionId(sectionId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
