package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.Customer;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository {
    Customer save(Customer customer);
    Optional<Customer> findByUserId(UUID userId);
}
