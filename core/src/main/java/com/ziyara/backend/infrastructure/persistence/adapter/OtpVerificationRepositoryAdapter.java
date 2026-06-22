package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.OtpVerification;
import com.ziyara.backend.domain.repository.OtpVerificationRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.OtpVerificationMapper;
import com.ziyara.backend.infrastructure.persistence.repository.OtpVerificationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OtpVerificationRepositoryAdapter implements OtpVerificationRepository {

    private final OtpVerificationJpaRepository jpaRepository;
    private final OtpVerificationMapper mapper;

    @Override
    public OtpVerification save(OtpVerification otp) {
        return mapper.toDomainEntity(jpaRepository.save(mapper.toJpaEntity(otp)));
    }

    @Override
    public Optional<OtpVerification> findValidByEmailOrPhoneAndOtp(String emailOrPhone, String otp, Instant now) {
        return jpaRepository.findByEmailOrPhoneAndOtpAndExpiresAtAfter(emailOrPhone, otp, now)
                .map(mapper::toDomainEntity);
    }

    @Override
    public void deleteByEmailOrPhone(String emailOrPhone) {
        jpaRepository.deleteByEmailOrPhone(emailOrPhone);
    }

    @Override
    public void deleteExpiredBefore(Instant cutoff) {
        jpaRepository.deleteByExpiresAtBefore(cutoff);
    }
}
