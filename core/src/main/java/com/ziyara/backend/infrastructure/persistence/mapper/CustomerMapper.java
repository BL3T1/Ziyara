package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.Customer;
import com.ziyara.backend.infrastructure.persistence.entity.CustomerJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public Customer toDomain(CustomerJpaEntity entity) {
        if (entity == null) return null;
        Customer customer = new Customer();
        customer.setUserId(entity.getUserId());
        customer.setFirstName(entity.getFirstName());
        customer.setLastName(entity.getLastName());
        customer.setIdDocumentUrl(entity.getIdDocumentUrl());
        customer.setIdDocumentType(entity.getIdDocumentType());
        customer.setIdDocumentNumber(entity.getIdDocumentNumber());
        customer.setPreferredCurrency(entity.getPreferredCurrency());
        customer.setProfileImageUrl(entity.getProfileImageUrl());
        customer.setDateOfBirth(entity.getDateOfBirth());
        customer.setNationality(entity.getNationality());
        customer.setCreatedAt(entity.getCreatedAt());
        customer.setUpdatedAt(entity.getUpdatedAt());
        return customer;
    }

    public CustomerJpaEntity toJpa(Customer customer) {
        if (customer == null) return null;
        return CustomerJpaEntity.builder()
                .userId(customer.getUserId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .idDocumentUrl(customer.getIdDocumentUrl())
                .idDocumentType(customer.getIdDocumentType())
                .idDocumentNumber(customer.getIdDocumentNumber())
                .preferredCurrency(customer.getPreferredCurrency())
                .profileImageUrl(customer.getProfileImageUrl())
                .dateOfBirth(customer.getDateOfBirth())
                .nationality(customer.getNationality())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
