package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.ContactLead;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ContactLeadRepository {

    ContactLead save(ContactLead lead);

    Optional<ContactLead> findById(UUID id);

    long countByEmailSince(String email, Instant since);

    long countByEmailAndIpSince(String email, String ip, Instant since);

    void deleteById(UUID id);
}
