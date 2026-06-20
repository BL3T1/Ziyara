package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.OtpVerification;

import java.time.Instant;
import java.util.Optional;

public interface OtpVerificationRepository {

    OtpVerification save(OtpVerification otp);

    Optional<OtpVerification> findValidByEmailOrPhoneAndOtp(String emailOrPhone, String otp, Instant now);

    void deleteByEmailOrPhone(String emailOrPhone);

    void deleteExpiredBefore(Instant cutoff);
}
