package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.OtpVerification;
import com.ziyara.backend.infrastructure.persistence.entity.OtpVerificationJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OtpVerificationMapper {

    public OtpVerification toDomainEntity(OtpVerificationJpaEntity entity) {
        if (entity == null) return null;
        OtpVerification otp = new OtpVerification();
        otp.setId(entity.getId());
        otp.setEmailOrPhone(entity.getEmailOrPhone());
        otp.setOtp(entity.getOtp());
        otp.setExpiresAt(entity.getExpiresAt());
        otp.setCreatedAt(entity.getCreatedAt());
        return otp;
    }

    public OtpVerificationJpaEntity toJpaEntity(OtpVerification otp) {
        if (otp == null) return null;
        return OtpVerificationJpaEntity.builder()
                .id(otp.getId())
                .emailOrPhone(otp.getEmailOrPhone())
                .otp(otp.getOtp())
                .expiresAt(otp.getExpiresAt())
                .createdAt(otp.getCreatedAt())
                .build();
    }
}
