package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.UserConsent;
import com.ziyara.backend.domain.repository.UserConsentRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.UserConsentMapper;
import com.ziyara.backend.infrastructure.persistence.repository.UserConsentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserConsentRepositoryAdapter implements UserConsentRepository {

    private final UserConsentJpaRepository jpaRepository;
    private final UserConsentMapper mapper;

    @Override
    public UserConsent save(UserConsent consent) {
        return mapper.toDomainEntity(jpaRepository.save(mapper.toJpaEntity(consent)));
    }

    @Override
    public List<UserConsent> findByUserIdOrderedDesc(UUID userId) {
        return jpaRepository.findByUserIdOrderByGrantedAtDesc(userId).stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }
}
