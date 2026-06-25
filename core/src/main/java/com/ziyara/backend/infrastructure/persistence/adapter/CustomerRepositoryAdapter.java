package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.Customer;
import com.ziyara.backend.domain.repository.CustomerRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.CustomerMapper;
import com.ziyara.backend.infrastructure.persistence.repository.CustomerJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CustomerRepositoryAdapter implements CustomerRepository {

    private final CustomerJpaRepository jpaRepository;
    private final CustomerMapper mapper;

    @Override
    public Customer save(Customer customer) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(customer)));
    }

    @Override
    public Optional<Customer> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    public List<Customer> findPendingIdentityVerifications() {
        return jpaRepository.findByIdDocumentUrlNotNullAndIdentityVerified(false)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Customer> findVerifiedIdentityDocuments() {
        return jpaRepository.findByIdentityVerifiedTrue()
                .stream().map(mapper::toDomain).toList();
    }
}
