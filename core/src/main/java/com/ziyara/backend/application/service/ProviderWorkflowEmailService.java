package com.ziyara.backend.application.service;

import com.ziyara.backend.domain.entity.ServiceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Email notifications for provider onboarding workflow.
 * Safe by default: disabled unless app.notifications.email.enabled=true.
 */
@Service
@Slf4j
public class ProviderWorkflowEmailService {

    private final MailDispatchService mailDispatchService;
    private final boolean enabled;
    private final String fromAddress;
    private final String logoUrl;
    private final List<String> approverRecipients;

    public ProviderWorkflowEmailService(
            MailDispatchService mailDispatchService,
            @Value("${app.notifications.email.enabled:false}") boolean enabled,
            @Value("${app.notifications.email.from:no-reply@ziyara.local}") String fromAddress,
            @Value("${app.notifications.email.public-landing-base-url:http://www.local}") String publicLandingBaseUrl,
            @Value("${app.notifications.email.provider-approver-recipients:}") String approverRecipientsCsv
    ) {
        this.mailDispatchService = mailDispatchService;
        this.enabled = enabled;
        this.fromAddress = fromAddress;
        this.logoUrl = trimTrailingSlash(publicLandingBaseUrl) + "/logo.png";
        this.approverRecipients = Arrays.stream(approverRecipientsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    public void notifySubmittedForApproval(ServiceProvider provider, String managerEmail) {
        String managerContent = "<p style=\"margin:0 0 16px;font-size:15px;color:#374151;line-height:1.6;\">"
                + "Your provider account has been successfully submitted and is currently under review by our team.</p>"
                + infoBox("Provider", escapeHtml(provider.getName()), "Status", "Pending Approval")
                + "<p style=\"margin:0;font-size:14px;color:#6b7280;line-height:1.6;\">"
                + "You will receive an email notification as soon as a decision has been made. "
                + "If you have any questions in the meantime, please don't hesitate to reach out to us.</p>";
        send(managerEmail, "Provider submission received — " + provider.getName(),
                buildFrame("We've Received Your Submission", managerContent));

        if (!approverRecipients.isEmpty()) {
            String adminContent = "<p style=\"margin:0 0 16px;font-size:15px;color:#374151;line-height:1.6;\">"
                    + "A new provider account is awaiting your review and approval.</p>"
                    + infoBox("Provider", escapeHtml(provider.getName()), "Status", "Pending Approval")
                    + "<p style=\"margin:0;font-size:14px;color:#6b7280;\">Please review this provider in "
                    + "<strong>Management → Providers</strong> at your earliest convenience.</p>";
            send(approverRecipients.toArray(String[]::new),
                    "Provider approval requested — " + provider.getName(),
                    buildFrame("New Provider Awaiting Approval", adminContent));
        }
    }

    public void notifyActivated(ServiceProvider provider, String managerEmail) {
        String content = "<p style=\"margin:0 0 16px;font-size:15px;color:#374151;line-height:1.6;\">"
                + "Great news! Your provider account has been reviewed and approved. You can now access the provider portal.</p>"
                + infoBox("Provider", escapeHtml(provider.getName()), "Status", "Active")
                + "<div style=\"text-align:center;margin:24px 0;\">"
                + "<a href=\"" + escapeHtml(fromAddress.contains("@") ? "#" : "#") + "\" "
                + "style=\"display:inline-block;background:#1a3a42;color:#ffffff;font-size:15px;font-weight:600;"
                + "text-decoration:none;padding:14px 36px;border-radius:8px;\">Go to Provider Portal</a>"
                + "</div>"
                + "<p style=\"margin:0;font-size:14px;color:#6b7280;line-height:1.6;\">"
                + "If you have any questions or need assistance getting started, feel free to contact our support team.</p>";
        send(managerEmail, "Your Ziyara provider account is now active — " + provider.getName(),
                buildFrame("Your Account is Approved!", content));
    }

    public void notifyExpiryWarningToAdmins(ServiceProvider provider) {
        if (approverRecipients.isEmpty()) return;
        String content = "<p style=\"margin:0 0 16px;font-size:15px;color:#374151;line-height:1.6;\">"
                + "The following partner account will expire in <strong>7 days</strong>. "
                + "The manager will lose portal access unless the subscription is renewed.</p>"
                + infoBox("Partner", escapeHtml(provider.getName()), "Expiry Date", String.valueOf(provider.getExpiryDate()))
                + "<p style=\"margin:0;font-size:14px;color:#6b7280;\">Please renew via "
                + "<strong>Management → Providers</strong>.</p>";
        send(approverRecipients.toArray(String[]::new),
                "Partner account expiring in 7 days — " + provider.getName(),
                buildFrame("Partner Account Expiry Warning", content));
    }

    public void notifyExpiredToAdmins(ServiceProvider provider) {
        if (approverRecipients.isEmpty()) return;
        String content = "<p style=\"margin:0 0 16px;font-size:15px;color:#374151;line-height:1.6;\">"
                + "The following partner account has <strong>expired</strong>. "
                + "The manager can no longer log in to the portal.</p>"
                + infoBox("Partner", escapeHtml(provider.getName()), "Expired On", String.valueOf(provider.getExpiryDate()))
                + "<p style=\"margin:0;font-size:14px;color:#6b7280;\">Please update the expiry date in "
                + "<strong>Management → Providers</strong> to restore access.</p>";
        send(approverRecipients.toArray(String[]::new),
                "Partner account expired — " + provider.getName(),
                buildFrame("Partner Account Has Expired", content));
    }

    public void notifyRejected(ServiceProvider provider, String managerEmail, String reason) {
        String reasonHtml = (reason == null || reason.isBlank()) ? "" :
                "<div style=\"background:#fff7ed;border:1.5px solid #fdba74;border-radius:10px;"
                + "padding:16px 20px;margin:16px 0;\">"
                + "<p style=\"margin:0 0 4px;font-size:12px;color:#6b7280;text-transform:uppercase;"
                + "letter-spacing:.04em;\">Reason</p>"
                + "<p style=\"margin:0;font-size:14px;color:#374151;\">" + escapeHtml(reason.trim()) + "</p>"
                + "</div>";
        String content = "<p style=\"margin:0 0 16px;font-size:15px;color:#374151;line-height:1.6;\">"
                + "Unfortunately, your provider submission could not be approved at this time.</p>"
                + infoBox("Provider", escapeHtml(provider.getName()), "Status", "Not Approved")
                + reasonHtml
                + "<p style=\"margin:0;font-size:14px;color:#6b7280;line-height:1.6;\">"
                + "Please contact our support team for guidance on how to resubmit with the required corrections.</p>";
        send(managerEmail, "Provider submission not approved — " + provider.getName(),
                buildFrame("Submission Update", content));
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private String infoBox(String label1, String value1, String label2, String value2) {
        return "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;"
                + "margin:0 0 20px;overflow:hidden;\">"
                + "<tr>"
                + "<td style=\"padding:14px 20px;border-right:1px solid #e2e8f0;width:50%;\">"
                + "<p style=\"margin:0 0 3px;font-size:12px;color:#9ca3af;text-transform:uppercase;"
                + "letter-spacing:.04em;\">" + label1 + "</p>"
                + "<p style=\"margin:0;font-size:14px;font-weight:600;color:#1a3a42;\">" + value1 + "</p>"
                + "</td>"
                + "<td style=\"padding:14px 20px;width:50%;\">"
                + "<p style=\"margin:0 0 3px;font-size:12px;color:#9ca3af;text-transform:uppercase;"
                + "letter-spacing:.04em;\">" + label2 + "</p>"
                + "<p style=\"margin:0;font-size:14px;font-weight:600;color:#1a3a42;\">" + value2 + "</p>"
                + "</td>"
                + "</tr></table>";
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

                + "<tr><td style=\"background:#1a3a42;padding:28px 40px;text-align:center;\">"
                + "<img src=\"" + logoUrl + "\" alt=\"Ziyara\" height=\"48\" "
                + "style=\"display:block;margin:0 auto;max-width:160px;\" /></td></tr>"

                + "<tr><td style=\"padding:32px 40px 0;\">"
                + "<h1 style=\"margin:0;font-size:22px;font-weight:700;color:#1a3a42;\">"
                + escapeHtml(headline) + "</h1>"
                + "<div style=\"width:40px;height:3px;background:#3d7080;border-radius:2px;margin:10px 0 24px;\"></div>"
                + "</td></tr>"

                + "<tr><td style=\"padding:0 40px 36px;\">" + innerContent + "</td></tr>"

                + "<tr><td style=\"background:#f9fafb;border-top:1px solid #e8ecef;"
                + "padding:22px 40px;text-align:center;\">"
                + "<p style=\"margin:0 0 6px;font-size:13px;color:#6b7280;\">"
                + "Questions? Email us at "
                + "<a href=\"mailto:" + escapeHtml(fromAddress) + "\" "
                + "style=\"color:#3d7080;text-decoration:none;font-weight:600;\">"
                + escapeHtml(fromAddress) + "</a></p>"
                + "<p style=\"margin:0;font-size:12px;color:#9ca3af;\">"
                + "&copy; 2025 Ziyara. All rights reserved.</p>"
                + "</td></tr>"

                + "</table></td></tr></table></body></html>";
    }

    private void send(String to, String subject, String htmlBody) {
        if (to == null || to.isBlank()) return;
        send(new String[]{to.trim()}, subject, htmlBody);
    }

    private void send(String[] to, String subject, String htmlBody) {
        if (!enabled) {
            log.info("Email notifications disabled; skipped subject='{}' recipients={}", subject, Arrays.toString(to));
            return;
        }
        log.info("Email queued subject='{}' recipients={}", subject, Arrays.toString(to));
        mailDispatchService.sendHtml(fromAddress, fromAddress, to, subject, htmlBody);
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) return "http://www.local";
        String u = url.trim();
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u.isEmpty() ? "http://www.local" : u;
    }
}
