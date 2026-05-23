package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.PublicContactRequest;
import com.ziyara.backend.domain.entity.ContactLead;
import com.ziyara.backend.domain.repository.ContactLeadRepository;
import com.ziyara.backend.application.exception.RateLimitedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ContactLeadService {

    private static final int COOLDOWN_SECONDS = 60;

    private final ContactLeadRepository repository;

    @Transactional
    public void submit(PublicContactRequest request, String clientIp) {
        String email = request.getEmail().trim();
        Instant since = Instant.now().minusSeconds(COOLDOWN_SECONDS);
        if (repository.countByEmailSince(email, since) > 0) {
            throw new RateLimitedException("Please wait before submitting again with the same email.");
        }
        ContactLead lead = new ContactLead();
        lead.setName(request.getName().trim());
        lead.setEmail(email);
        lead.setCompany(request.getCompany() != null ? request.getCompany().trim() : null);
        lead.setMessage(request.getMessage().trim());
        lead.setClientIp(truncateIp(clientIp));
        lead.setCreatedAt(Instant.now());
        repository.save(lead);
    }

    private static String truncateIp(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.length() > 64 ? raw.substring(0, 64) : raw;
    }
}
