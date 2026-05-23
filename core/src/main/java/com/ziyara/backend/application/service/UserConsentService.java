package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.UserConsentResponse;
import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.entity.UserConsent;
import com.ziyara.backend.domain.repository.UserConsentRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserConsentService {

    private final UserConsentRepository consentRepository;
    private final UserRepository userRepository;

    public List<UserConsentResponse> list(UUID userId) {
        return consentRepository.findByUserIdOrderedDesc(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserConsentResponse recordGrant(UUID userId, String consentType, String purpose, boolean granted, String ip, String ua) {
        int nextVersion = consentRepository.findByUserIdOrderedDesc(userId).stream()
                .filter(c -> consentType.equalsIgnoreCase(c.getConsentType()))
                .mapToInt(c -> c.getVersion() != null ? c.getVersion() : 1)
                .max()
                .orElse(0) + 1;

        UserConsent consent = new UserConsent();
        consent.setUserId(userId);
        consent.setConsentType(consentType);
        consent.setPurpose(purpose);
        consent.setGranted(granted);
        consent.setGrantedAt(LocalDateTime.now());
        consent.setVersion(nextVersion);
        consent.setIpAddress(ip);
        consent.setUserAgent(ua);
        UserConsent saved = consentRepository.save(consent);

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
        return toResponse(saved);
    }

    @Transactional
    public void withdraw(UUID userId, String consentType, String reason) {
        List<UserConsent> consents = consentRepository.findByUserIdOrderedDesc(userId);
        UserConsent latest = consents.stream()
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

    private UserConsentResponse toResponse(UserConsent c) {
        return UserConsentResponse.builder()
                .id(c.getId())
                .userId(c.getUserId())
                .consentType(c.getConsentType())
                .purpose(c.getPurpose())
                .granted(c.getGranted())
                .grantedAt(c.getGrantedAt())
                .withdrawnAt(c.getWithdrawnAt())
                .withdrawalReason(c.getWithdrawalReason())
                .version(c.getVersion())
                .ipAddress(c.getIpAddress())
                .userAgent(c.getUserAgent())
                .build();
    }
}
