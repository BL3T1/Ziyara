package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.ContentPage;
import com.ziyara.backend.domain.repository.ContentPageRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.ContentPageMapper;
import com.ziyara.backend.infrastructure.persistence.repository.ContentPageJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ContentPageRepositoryAdapter implements ContentPageRepository {

    private final ContentPageJpaRepository jpaRepository;
    private final ContentPageMapper mapper;

    @Override
    public ContentPage save(ContentPage page) {
        return mapper.toDomainEntity(jpaRepository.save(mapper.toJpaEntity(page)));
    }

    @Override
    public Optional<ContentPage> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public Optional<ContentPage> findBySlug(String slug) {
        return jpaRepository.findBySlug(slug).map(mapper::toDomainEntity);
    }

    @Override
    public List<ContentPage> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
