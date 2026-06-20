package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.RestMenuSection;
import com.ziyara.backend.domain.repository.RestMenuSectionRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.RestMenuSectionMapper;
import com.ziyara.backend.infrastructure.persistence.repository.RestMenuSectionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RestMenuSectionRepositoryAdapter implements RestMenuSectionRepository {

    private final RestMenuSectionJpaRepository jpaRepository;
    private final RestMenuSectionMapper mapper;

    @Override
    public RestMenuSection save(RestMenuSection section) {
        return mapper.toDomainEntity(jpaRepository.save(mapper.toJpaEntity(section)));
    }

    @Override
    public Optional<RestMenuSection> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public List<RestMenuSection> findByServiceId(UUID serviceId) {
        return jpaRepository.findByServiceIdOrderBySortOrderAscIdAsc(serviceId).stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long countByServiceId(UUID serviceId) {
        return jpaRepository.countByServiceId(serviceId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
