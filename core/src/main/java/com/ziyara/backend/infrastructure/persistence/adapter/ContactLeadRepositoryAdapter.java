package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.ContactLead;
import com.ziyara.backend.domain.repository.ContactLeadRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.ContactLeadMapper;
import com.ziyara.backend.infrastructure.persistence.repository.ContactLeadJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ContactLeadRepositoryAdapter implements ContactLeadRepository {

    private final ContactLeadJpaRepository jpaRepository;
    private final ContactLeadMapper mapper;

    @Override
    public ContactLead save(ContactLead lead) {
        return mapper.toDomainEntity(jpaRepository.save(mapper.toJpaEntity(lead)));
    }

    @Override
    public Optional<ContactLead> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public long countByEmailSince(String email, Instant since) {
        return jpaRepository.countByEmailIgnoreCaseAndCreatedAtAfter(email, since);
    }

    @Override
    public long countByEmailAndIpSince(String email, String ip, Instant since) {
        return jpaRepository.countByEmailIgnoreCaseAndIpAddressAndCreatedAtAfter(email, ip, since);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
