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
        String ip = truncateIp(clientIp);
        Instant since = Instant.now().minusSeconds(COOLDOWN_SECONDS);

        // Prefer the combined email+IP check to block per-device; fall back to email-only
        // when IP is unavailable (e.g. internal tooling, unit tests with null IP).
        long recentCount = (ip != null)
                ? repository.countByEmailAndIpSince(email, ip, since)
                : repository.countByEmailSince(email, since);

        if (recentCount > 0) {
            throw new RateLimitedException("Please wait before submitting again with the same email.");
        }
        ContactLead lead = new ContactLead();
        lead.setName(request.getName().trim());
        lead.setEmail(email);
        lead.setCompany(request.getCompany() != null ? request.getCompany().trim() : null);
        lead.setMessage(request.getMessage().trim());
        lead.setIpAddress(ip);
        lead.setCreatedAt(Instant.now());
        repository.save(lead);
    }

    private static String truncateIp(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.length() > 45 ? raw.substring(0, 45) : raw;
    }
}
