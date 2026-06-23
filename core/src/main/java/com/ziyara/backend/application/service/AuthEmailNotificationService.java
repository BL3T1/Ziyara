package com.ziyara.backend.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final String logoUrl;

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
        this.logoUrl = this.publicLandingBaseUrl + "/logo.png";
    }

    public void sendSignupOtp(String toEmail, String code) {
        if (toEmail == null || toEmail.isBlank()) return;
        String body = buildOtpBody(
                "Welcome to Ziyara!",
                "Thank you for joining us. To complete your registration, please use the verification code below.",
                code,
                "This code expires in 10 minutes. If you did not create an account, you can safely ignore this email."
        );
        send(toEmail.trim(), "Your Ziyara verification code", body);
    }

    public void sendOtpCode(String toEmail, String code) {
        if (toEmail == null || toEmail.isBlank()) return;
        String body = buildOtpBody(
                "Verify Your Email",
                "We received a request to verify your email address. Use the code below to proceed.",
                code,
                "This code expires in 10 minutes. If you did not request this, you can safely ignore this email."
        );
        send(toEmail.trim(), "Your Ziyara verification code", body);
    }

    public void sendTempPasswordReset(String toEmail, String tempPassword) {
        if (toEmail == null || toEmail.isBlank()) return;
        String content = "<p style=\"margin:0 0 16px;font-size:15px;color:#374151;line-height:1.6;\">"
                + "Your Ziyara partner portal password has been reset by an administrator.</p>"
                + "<div style=\"background:#f0fdf4;border:1.5px solid #86efac;border-radius:10px;padding:20px 24px;margin:0 0 20px;text-align:center;\">"
                + "<p style=\"margin:0 0 6px;font-size:13px;color:#6b7280;letter-spacing:.04em;text-transform:uppercase;\">Temporary Password</p>"
                + "<p style=\"margin:0;font-size:22px;font-weight:700;color:#1a3a42;letter-spacing:.08em;font-family:monospace;\">"
                + escapeHtml(tempPassword) + "</p>"
                + "</div>"
                + "<p style=\"margin:0;font-size:14px;color:#6b7280;line-height:1.6;\">"
                + "You will be required to set a new password on your next login. "
                + "If you did not request this change, please contact support immediately.</p>";
        String html = buildFrame("Portal Password Reset", content);
        send(toEmail.trim(), "Your Ziyara portal password has been reset", html);
    }

    public void sendPasswordReset(String toEmail, String token) {
        if (toEmail == null || toEmail.isBlank() || token == null || token.isBlank()) return;
        String path = "/reset-password?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        String link = publicLandingBaseUrl + path;
        String content = "<p style=\"margin:0 0 16px;font-size:15px;color:#374151;line-height:1.6;\">"
                + "We received a request to reset your Ziyara password. Click the button below to choose a new password. "
                + "This link is valid for <strong>60 minutes</strong>.</p>"
                + "<div style=\"text-align:center;margin:28px 0;\">"
                + "<a href=\"" + link + "\" "
                + "style=\"display:inline-block;background:#1a3a42;color:#ffffff;font-size:15px;font-weight:600;"
                + "text-decoration:none;padding:14px 36px;border-radius:8px;letter-spacing:.02em;\">"
                + "Reset My Password</a>"
                + "</div>"
                + "<p style=\"margin:0 0 8px;font-size:13px;color:#9ca3af;\">Button not working? Copy and paste this link into your browser:</p>"
                + "<p style=\"margin:0;font-size:12px;color:#3d7080;word-break:break-all;\">" + escapeHtml(link) + "</p>"
                + "<hr style=\"border:none;border-top:1px solid #e8ecef;margin:24px 0;\">"
                + "<p style=\"margin:0;font-size:13px;color:#9ca3af;\">If you did not request a password reset, no action is needed — your password remains unchanged.</p>";
        String html = buildFrame("Reset Your Password", content);
        send(toEmail.trim(), "Reset your Ziyara password", html);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private String buildOtpBody(String headline, String intro, String code, String note) {
        String content = "<p style=\"margin:0 0 20px;font-size:15px;color:#374151;line-height:1.6;\">"
                + escapeHtml(intro) + "</p>"
                + "<div style=\"background:#f0f9ff;border:1.5px solid #7dd3fc;border-radius:10px;padding:24px;margin:0 0 20px;text-align:center;\">"
                + "<p style=\"margin:0 0 6px;font-size:13px;color:#6b7280;letter-spacing:.06em;text-transform:uppercase;\">Verification Code</p>"
                + "<p style=\"margin:0;font-size:36px;font-weight:700;color:#1a3a42;letter-spacing:.18em;font-family:monospace;\">"
                + escapeHtml(code) + "</p>"
                + "</div>"
                + "<p style=\"margin:0;font-size:13px;color:#9ca3af;line-height:1.6;\">"
                + escapeHtml(note) + "</p>";
        return buildFrame(headline, content);
    }

    private String buildFrame(String headline, String innerContent) {
        return "<!DOCTYPE html>"
                + "<html lang=\"en\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">"
                + "<title>" + escapeHtml(headline) + "</title></head>"
                + "<body style=\"margin:0;padding:0;background:#f4f6f8;"
                + "font-family:'Helvetica Neue',Arial,sans-serif;\">"
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background:#f4f6f8;padding:40px 16px;\">"
                + "<tr><td align=\"center\">"
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"max-width:560px;background:#ffffff;border-radius:14px;overflow:hidden;"
                + "box-shadow:0 4px 20px rgba(0,0,0,0.08);\">"

                // ── Header ─────────────────────────────────────────────
                + "<tr><td style=\"background:#1a3a42;padding:28px 40px;text-align:center;\">"
                + "<img src=\"" + logoUrl + "\" alt=\"Ziyara\" height=\"48\" "
                + "style=\"display:block;margin:0 auto;max-width:160px;\" />"
                + "</td></tr>"

                // ── Headline ───────────────────────────────────────────
                + "<tr><td style=\"padding:32px 40px 0;\">"
                + "<h1 style=\"margin:0;font-size:22px;font-weight:700;color:#1a3a42;\">"
                + escapeHtml(headline) + "</h1>"
                + "<div style=\"width:40px;height:3px;background:#3d7080;border-radius:2px;margin:10px 0 24px;\"></div>"
                + "</td></tr>"

                // ── Body ───────────────────────────────────────────────
                + "<tr><td style=\"padding:0 40px 36px;\">"
                + innerContent
                + "</td></tr>"

                // ── Footer ─────────────────────────────────────────────
                + "<tr><td style=\"background:#f9fafb;border-top:1px solid #e8ecef;"
                + "padding:22px 40px;text-align:center;\">"
                + "<p style=\"margin:0 0 6px;font-size:13px;color:#6b7280;\">"
                + "Questions? Email us at "
                + "<a href=\"mailto:" + escapeHtml(fromAddress) + "\" "
                + "style=\"color:#3d7080;text-decoration:none;font-weight:600;\">"
                + escapeHtml(fromAddress) + "</a>"
                + "</p>"
                + "<p style=\"margin:0;font-size:12px;color:#9ca3af;\">"
                + "&copy; 2025 Ziyara. All rights reserved.</p>"
                + "</td></tr>"

                + "</table>"
                + "</td></tr></table>"
                + "</body></html>";
    }

    private void send(String to, String subject, String htmlBody) {
        if (!enabled) {
            log.debug("Auth email skipped (notifications disabled): subject='{}' to={}", subject, to);
            return;
        }
        log.info("Auth email queued subject='{}' to={}", subject, to);
        mailDispatchService.sendHtml(fromAddress, fromAddress, to, subject, htmlBody);
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) return "http://www.local";
        String u = url.trim();
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u.isEmpty() ? "http://www.local" : u;
    }
}
