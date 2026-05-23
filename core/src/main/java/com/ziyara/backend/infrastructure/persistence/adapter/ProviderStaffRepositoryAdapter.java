package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.ProviderStaff;
import com.ziyara.backend.domain.repository.ProviderStaffRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.ProviderStaffMapper;
import com.ziyara.backend.infrastructure.persistence.repository.ProviderStaffJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProviderStaffRepositoryAdapter implements ProviderStaffRepository {

    private final ProviderStaffJpaRepository jpaRepository;
    private final ProviderStaffMapper mapper;

    @Override
    public ProviderStaff save(ProviderStaff staff) {
        return mapper.toDomainEntity(jpaRepository.save(mapper.toJpaEntity(staff)));
    }

    @Override
    public Optional<ProviderStaff> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public List<ProviderStaff> findByProviderId(UUID providerId) {
        return jpaRepository.findByProviderIdOrderByCreatedAtAsc(providerId).stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ProviderStaff> findByProviderIdAndUserId(UUID providerId, UUID userId) {
        return jpaRepository.findByProviderIdAndUserId(providerId, userId)
                .map(mapper::toDomainEntity);
    }

    @Override
    public Optional<ProviderStaff> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).map(mapper::toDomainEntity);
    }

    @Override
    public long countByProviderId(UUID providerId) {
        return jpaRepository.countByProviderId(providerId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
