package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.ContactLeadJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface ContactLeadJpaRepository extends JpaRepository<ContactLeadJpaEntity, UUID> {
    long countByEmailIgnoreCaseAndCreatedAtAfter(String email, Instant after);
}
