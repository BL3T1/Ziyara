package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.DataExportRequest;
import com.ziyara.backend.domain.repository.DataExportRequestRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.DataExportRequestMapper;
import com.ziyara.backend.infrastructure.persistence.repository.DataExportRequestJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataExportRequestRepositoryAdapter implements DataExportRequestRepository {

    private final DataExportRequestJpaRepository jpaRepository;
    private final DataExportRequestMapper mapper;

    @Override
    public DataExportRequest save(DataExportRequest request) {
        return mapper.toDomainEntity(jpaRepository.save(mapper.toJpaEntity(request)));
    }

    @Override
    public Optional<DataExportRequest> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public List<DataExportRequest> findByUserIdOrderedDesc(UUID userId) {
        return jpaRepository.findByUserIdOrderByRequestedAtDesc(userId).stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }
}
