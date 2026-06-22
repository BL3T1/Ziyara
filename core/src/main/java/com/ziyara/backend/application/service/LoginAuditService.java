package com.ziyara.backend.application.service;

import com.ziyara.backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Records successful-login metadata (lastLoginAt, lastLoginIp, failedLoginAttempts reset)
 * asynchronously so the auth transaction can commit and return the JWT without holding
 * a row-level write lock on the users table.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAuditService {

    private final UserRepository userRepository;

    @Async("loginAuditExecutor")
    @Transactional
    public void recordSuccessfulLogin(UUID userId, String ipAddress) {
        userRepository.findById(userId).ifPresent(user -> {
            user.recordSuccessfulLogin(ipAddress);
            userRepository.save(user);
        });
    }
}
