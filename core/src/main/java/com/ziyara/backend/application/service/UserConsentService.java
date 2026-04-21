package com.ziyara.backend.application.service;

import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.infrastructure.persistence.entity.UserConsentJpaEntity;
import com.ziyara.backend.infrastructure.persistence.repository.UserConsentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserConsentService {

    private final UserConsentJpaRepository consentRepository;
    private final UserRepository userRepository;

    public List<UserConsentJpaEntity> list(UUID userId) {
        return consentRepository.findByUserIdOrderByGrantedAtDesc(userId);
    }

    @Transactional
    public UserConsentJpaEntity recordGrant(UUID userId, String consentType, String purpose, boolean granted, String ip, String ua) {
        int nextVersion = consentRepository.findByUserIdOrderByGrantedAtDesc(userId).stream()
                .filter(c -> consentType.equalsIgnoreCase(c.getConsentType()))
                .mapToInt(c -> c.getVersion() != null ? c.getVersion() : 1)
                .max()
                .orElse(0) + 1;

        UserConsentJpaEntity e = UserConsentJpaEntity.builder()
                .userId(userId)
                .consentType(consentType)
                .purpose(purpose)
                .granted(granted)
                .grantedAt(LocalDateTime.now())
                .version(nextVersion)
                .ipAddress(ip)
                .userAgent(ua)
                .build();
        UserConsentJpaEntity saved = consentRepository.save(e);

        if ("DATA_PROCESSING".equalsIgnoreCase(consentType) && granted) {
            User u = userRepository.findById(userId).orElseThrow();
            u.setGdprConsentGiven(true);
            u.setGdprConsentDate(LocalDateTime.now());
            userRepository.save(u);
        }
        if ("MARKETING_EMAIL".equalsIgnoreCase(consentType)) {
            User u = userRepository.findById(userId).orElseThrow();
            u.setMarketingOptIn(granted);
            userRepository.save(u);
        }
        return saved;
    }

    @Transactional
    public void withdraw(UUID userId, String consentType, String reason) {
        List<UserConsentJpaEntity> rows = consentRepository.findByUserIdOrderByGrantedAtDesc(userId);
        UserConsentJpaEntity latest = rows.stream()
                .filter(c -> consentType.equalsIgnoreCase(c.getConsentType()) && c.getWithdrawnAt() == null)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No active consent for type: " + consentType));
        latest.setWithdrawnAt(LocalDateTime.now());
        latest.setWithdrawalReason(reason);
        consentRepository.save(latest);

        if ("MARKETING_EMAIL".equalsIgnoreCase(consentType)) {
            User u = userRepository.findById(userId).orElseThrow();
            u.setMarketingOptIn(false);
            userRepository.save(u);
        }
    }
}
