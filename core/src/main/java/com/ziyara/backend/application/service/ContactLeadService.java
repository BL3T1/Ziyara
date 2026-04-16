package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.PublicContactRequest;
import com.ziyara.backend.infrastructure.persistence.entity.ContactLeadJpaEntity;
import com.ziyara.backend.infrastructure.persistence.repository.ContactLeadJpaRepository;
import com.ziyara.backend.presentation.exception.RateLimitedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ContactLeadService {

    private static final int COOLDOWN_SECONDS = 60;

    private final ContactLeadJpaRepository repository;

    @Transactional
    public void submit(PublicContactRequest request, String clientIp) {
        String email = request.getEmail().trim();
        Instant since = Instant.now().minusSeconds(COOLDOWN_SECONDS);
        if (repository.countByEmailIgnoreCaseAndCreatedAtAfter(email, since) > 0) {
            throw new RateLimitedException("Please wait before submitting again with the same email.");
        }
        ContactLeadJpaEntity row = ContactLeadJpaEntity.builder()
                .name(request.getName().trim())
                .email(email)
                .company(request.getCompany() != null ? request.getCompany().trim() : null)
                .message(request.getMessage().trim())
                .clientIp(truncateIp(clientIp))
                .createdAt(Instant.now())
                .build();
        repository.save(row);
    }

    private static String truncateIp(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.length() > 64 ? raw.substring(0, 64) : raw;
    }
}
