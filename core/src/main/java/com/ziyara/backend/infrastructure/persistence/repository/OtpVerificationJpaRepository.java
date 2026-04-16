package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.OtpVerificationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OtpVerificationJpaRepository extends JpaRepository<OtpVerificationJpaEntity, UUID> {

    @Query("SELECT o FROM OtpVerificationJpaEntity o WHERE o.emailOrPhone = :emailOrPhone AND o.otp = :otp AND o.expiresAt > :now")
    Optional<OtpVerificationJpaEntity> findByEmailOrPhoneAndOtpAndExpiresAtAfter(
            @Param("emailOrPhone") String emailOrPhone, @Param("otp") String otp, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM OtpVerificationJpaEntity o WHERE o.emailOrPhone = :emailOrPhone")
    void deleteByEmailOrPhone(@Param("emailOrPhone") String emailOrPhone);

    void deleteByExpiresAtBefore(Instant now);
}
