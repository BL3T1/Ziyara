package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.AuthRequest;
import com.ziyara.backend.application.dto.AuthResponse;
import com.ziyara.backend.application.dto.request.*;
import com.ziyara.backend.application.annotation.RateLimit;
import com.ziyara.backend.application.service.AuthService;
import com.ziyara.backend.application.service.LoginRateLimitService;
import com.ziyara.backend.application.service.SecurityAlertService;
import com.ziyara.backend.application.service.SecurityEventService;
import com.ziyara.backend.infrastructure.security.JwtCookieProperties;
import com.ziyara.backend.infrastructure.web.AuthCookieHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller: AuthController
 * Handles authentication endpoints (all command-side, JPA).
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimitService loginRateLimitService;
    private final SecurityEventService securityEventService;
    private final SecurityAlertService securityAlertService;
    private final JwtCookieProperties jwtCookieProperties;

    @Value("${server.servlet.context-path:/}")
    private String servletContextPath;

    @PostMapping("/register")
    @RateLimit(key = "POST:/auth/register", maxPerMinute = 10)
    @Operation(summary = "Register", description = "Register a new user (customer self-signup). Provider accounts must use provider onboarding.")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Registration successful. Please log in.", null));
    }

    @PostMapping("/password/forgot")
    @RateLimit(key = "POST:/auth/password/forgot", maxPerMinute = 5)
    @Operation(summary = "Forgot password", description = "Request password reset; token emailed when app.notifications.email.enabled=true")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("If the email exists, a reset link has been sent.", null));
    }

    @PostMapping("/password/reset")
    @RateLimit(key = "POST:/auth/password/reset", maxPerMinute = 5)
    @Operation(summary = "Reset password", description = "Reset password using token from forgot-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful.", null));
    }

    @PostMapping("/otp/send")
    @Operation(summary = "Send OTP", description = "Send OTP to email (emailed when enabled) or persist for phone")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody OtpSendRequest request) {
        authService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP sent.", null));
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Verify OTP", description = "Verify OTP code")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP verified.", null));
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody AuthRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String ipAddress = getClientIp(httpRequest);
        if (!loginRateLimitService.allow(ipAddress, "POST:/auth/login")) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("Too many login attempts from this network. Please try again shortly."));
        }
        try {
            AuthResponse response = authService.authenticate(request, ipAddress);
            if (jwtCookieProperties.isEnabled()) {
                AuthCookieHelper.addAuthCookies(
                        httpResponse,
                        response.getAccessToken(),
                        response.getRefreshToken(),
                        authService.accessTokenTtlSeconds(),
                        authService.refreshTokenTtlSeconds(),
                        servletContextPath,
                        jwtCookieProperties
                );
                if (!jwtCookieProperties.isAlsoReturnTokenInBody()) {
                    response = response.toBuilder()
                            .accessToken(null)
                            .refreshToken(null)
                            .build();
                }
            }
            return ResponseEntity.ok(ApiResponse.success("Login successful", response));
        } catch (AuthService.AuthenticationException ex) {
            Map<String, Object> details = new HashMap<>();
            details.put("email", request.getEmail());
            details.put("message", ex.getMessage());
            String eventType = ex.getMessage() != null && ex.getMessage().contains("MFA") ? "MFA_FAILED" : "LOGIN_FAILED";
            try {
                securityEventService.record(null, eventType, "LOW", ipAddress, httpRequest.getHeader("User-Agent"), details);
                securityAlertService.evaluateThresholdAlerts(ipAddress, eventType);
            } catch (RuntimeException sideEffect) {
                // Do not mask 401/403: security audit tables may be missing on older DBs or misconfigured envs.
                log.warn("Login failure audit skipped: {}", sideEffect.toString());
            }
            throw ex;
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Invalidate user session")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String access = bearerToken(request);
        if (access == null || access.isBlank()) {
            access = cookieValue(request, jwtCookieProperties.getAccessCookieName());
        }
        String refresh = refreshToken(request);
        if (refresh == null || refresh.isBlank()) {
            refresh = cookieValue(request, jwtCookieProperties.getRefreshCookieName());
        }
        authService.logout(access, refresh);
        if (jwtCookieProperties.isEnabled()) {
            AuthCookieHelper.clearAuthCookies(response, servletContextPath, jwtCookieProperties);
        }
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestHeader(value = "Refresh-Token", required = false) String refreshHeader,
            HttpServletRequest request,
            HttpServletResponse httpResponse
    ) {
        String refreshToken = refreshHeader != null && !refreshHeader.isBlank()
                ? refreshHeader.trim()
                : cookieValue(request, jwtCookieProperties.getRefreshCookieName());
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Refresh token required"));
        }
        AuthResponse authResponse = authService.refreshToken(refreshToken);
        if (jwtCookieProperties.isEnabled()) {
            AuthCookieHelper.addAuthCookies(
                    httpResponse,
                    authResponse.getAccessToken(),
                    authResponse.getRefreshToken(),
                    authService.accessTokenTtlSeconds(),
                    authService.refreshTokenTtlSeconds(),
                    servletContextPath,
                    jwtCookieProperties
            );
            if (!jwtCookieProperties.isAlsoReturnTokenInBody()) {
                authResponse = authResponse.toBuilder()
                        .accessToken(null)
                        .refreshToken(null)
                        .build();
            }
        }
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", authResponse));
    }

    private static String bearerToken(HttpServletRequest request) {
        String h = request.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            return h.substring(7).trim();
        }
        return null;
    }

    private static String cookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        for (var c : request.getCookies()) {
            if (name.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    private static String refreshToken(HttpServletRequest request) {
        String h = request.getHeader("Refresh-Token");
        if (h != null && !h.isBlank()) {
            return h.trim();
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}

