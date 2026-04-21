package com.ziyara.backend.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Transactional-style auth emails (signup OTP, password reset, generic OTP).
 * Respects {@code app.notifications.email.enabled}; when disabled, skips send (callers still log).
 */
@Service
@Slf4j
public class AuthEmailNotificationService {

    private final MailDispatchService mailDispatchService;
    private final boolean enabled;
    private final String fromAddress;
    private final String publicLandingBaseUrl;

    public AuthEmailNotificationService(
            MailDispatchService mailDispatchService,
            @Value("${app.notifications.email.enabled:false}") boolean enabled,
            @Value("${app.notifications.email.from:no-reply@ziyara.local}") String fromAddress,
            @Value("${app.notifications.email.public-landing-base-url:http://www.local}") String publicLandingBaseUrl
    ) {
        this.mailDispatchService = mailDispatchService;
        this.enabled = enabled;
        this.fromAddress = fromAddress;
        this.publicLandingBaseUrl = trimTrailingSlash(publicLandingBaseUrl);
    }

    public void sendSignupOtp(String toEmail, String code) {
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }
        String body = "Welcome to Ziyara.\n\n"
                + "Your verification code is: " + code + "\n\n"
                + "This code expires in 10 minutes. If you did not create an account, you can ignore this message.";
        send(toEmail.trim(), "Your Ziyara verification code", body);
    }

    public void sendOtpCode(String toEmail, String code) {
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }
        String body = "Your Ziyara verification code is: " + code + "\n\n"
                + "This code expires in 10 minutes.";
        send(toEmail.trim(), "Your Ziyara verification code", body);
    }

    public void sendPasswordReset(String toEmail, String token) {
        if (toEmail == null || toEmail.isBlank() || token == null || token.isBlank()) {
            return;
        }
        String path = "/reset-password?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        String link = publicLandingBaseUrl + path;
        String body = "We received a request to reset your Ziyara password.\n\n"
                + "Open this link to choose a new password (valid for 60 minutes):\n"
                + link + "\n\n"
                + "If the link does not work, copy your reset token and paste it on the reset page:\n"
                + token + "\n\n"
                + "If you did not request a reset, you can ignore this email.";
        send(toEmail.trim(), "Reset your Ziyara password", body);
    }

    private void send(String to, String subject, String body) {
        if (!enabled) {
            log.debug("Auth email skipped (notifications disabled): subject='{}' to={}", subject, to);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailDispatchService.send(message);
            log.info("Auth email queued subject='{}' to={}", subject, to);
        } catch (Exception ex) {
            log.warn("Auth email failed subject='{}' to={}: {}", subject, to, ex.getMessage());
        }
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://www.local";
        }
        String u = url.trim();
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u.isEmpty() ? "http://www.local" : u;
    }
}
