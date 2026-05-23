package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.PortalSupportRequest;
import com.ziyara.backend.domain.repository.PortalSupportRequestRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.PortalSupportRequestMapper;
import com.ziyara.backend.infrastructure.persistence.repository.PortalSupportRequestJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PortalSupportRequestRepositoryAdapter implements PortalSupportRequestRepository {

    private final PortalSupportRequestJpaRepository jpaRepository;
    private final PortalSupportRequestMapper mapper;

    @Override
    public PortalSupportRequest save(PortalSupportRequest request) {
        return mapper.toDomainEntity(jpaRepository.save(mapper.toJpaEntity(request)));
    }

    @Override
    public Optional<PortalSupportRequest> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public List<PortalSupportRequest> findByProviderId(UUID providerId) {
        return jpaRepository.findByProviderIdOrderByCreatedAtDesc(providerId).stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
