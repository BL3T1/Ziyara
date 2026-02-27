package com.ziyarah.application.service;

import com.ziyarah.application.dto.AuthRequest;
import com.ziyarah.application.dto.AuthResponse;
import com.ziyarah.domain.entity.User;
import com.ziyarah.domain.enums.UserStatus;
import com.ziyarah.domain.repository.UserRepository;
import com.ziyarah.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service: AuthService
 * Handles authentication business logic
 * Part of Clean Architecture - Application Layer
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional
    public AuthResponse authenticate(AuthRequest request, String ipAddress) {
        log.info("Authenticating user: {}", request.getEmail());
        
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));
        
        // Check if user can login
        if (!user.getStatus().canLogin()) {
            throw new AuthenticationException("Account is not active. Status: " + user.getStatus());
        }
        
        // Check if account is locked
        if (user.isLocked()) {
            throw new AuthenticationException("Account is temporarily locked. Please try again later.");
        }
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            // Increment failed attempts
            user.incrementFailedLoginAttempts();
            userRepository.save(user);
            throw new AuthenticationException("Invalid email or password");
        }
        
        // Record successful login
        user.recordSuccessfulLogin(ipAddress);
        userRepository.save(user);
        
        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        log.info("User authenticated successfully: {}", user.getEmail());
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
    
    @Transactional
    public void logout(String token) {
        // Token invalidation logic (could use Redis blacklist)
        log.info("User logged out");
    }
    
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtService.validateToken(refreshToken)) {
            throw new AuthenticationException("Invalid refresh token");
        }
        
        String userId = jwtService.extractUserId(refreshToken);
        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new AuthenticationException("User not found"));
        
        if (!user.isActive()) {
            throw new AuthenticationException("Account is not active");
        }
        
        String newAccessToken = jwtService.generateAccessToken(user);
        
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
    
    /**
     * Custom exception for authentication errors
     */
    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}
