package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.UserConsent;
import com.ziyara.backend.infrastructure.persistence.entity.UserConsentJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class UserConsentMapper {

    public UserConsent toDomainEntity(UserConsentJpaEntity entity) {
        if (entity == null) return null;
        UserConsent consent = new UserConsent();
        consent.setId(entity.getId());
        consent.setUserId(entity.getUserId());
        consent.setConsentType(entity.getConsentType());
        consent.setPurpose(entity.getPurpose());
        consent.setGranted(entity.getGranted());
        consent.setGrantedAt(entity.getGrantedAt());
        consent.setWithdrawnAt(entity.getWithdrawnAt());
        consent.setWithdrawalReason(entity.getWithdrawalReason());
        consent.setVersion(entity.getVersion());
        consent.setIpAddress(entity.getIpAddress());
        consent.setUserAgent(entity.getUserAgent());
        return consent;
    }

    public UserConsentJpaEntity toJpaEntity(UserConsent consent) {
        if (consent == null) return null;
        return UserConsentJpaEntity.builder()
                .id(consent.getId())
                .userId(consent.getUserId())
                .consentType(consent.getConsentType())
                .purpose(consent.getPurpose())
                .granted(consent.getGranted())
                .grantedAt(consent.getGrantedAt())
                .withdrawnAt(consent.getWithdrawnAt())
                .withdrawalReason(consent.getWithdrawalReason())
                .version(consent.getVersion())
                .ipAddress(consent.getIpAddress())
                .userAgent(consent.getUserAgent())
                .build();
    }
}
