package com.ziyara.backend.application.service;

import com.ziyara.backend.domain.entity.ServiceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
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
    private final List<String> approverRecipients;

    public ProviderWorkflowEmailService(
            MailDispatchService mailDispatchService,
            @Value("${app.notifications.email.enabled:false}") boolean enabled,
            @Value("${app.notifications.email.from:no-reply@ziyara.local}") String fromAddress,
            @Value("${app.notifications.email.provider-approver-recipients:}") String approverRecipientsCsv
    ) {
        this.mailDispatchService = mailDispatchService;
        this.enabled = enabled;
        this.fromAddress = fromAddress;
        this.approverRecipients = Arrays.stream(approverRecipientsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    public void notifySubmittedForApproval(ServiceProvider provider, String managerEmail) {
        send(managerEmail,
                "Provider submission received: " + provider.getName(),
                "Your provider account has been submitted for approval.\n\n"
                        + "Provider: " + provider.getName() + "\n"
                        + "Status: PENDING_APPROVAL\n"
                        + "You will receive another email after Super Admin/CEO review.");

        if (!approverRecipients.isEmpty()) {
            send(approverRecipients.toArray(String[]::new),
                    "Provider approval requested: " + provider.getName(),
                    "A new provider requires approval.\n\n"
                            + "Provider: " + provider.getName() + "\n"
                            + "Current status: PENDING_APPROVAL\n"
                            + "Please review in Management -> Providers.");
        }
    }

    public void notifyActivated(ServiceProvider provider, String managerEmail) {
        send(managerEmail,
                "Provider approved: " + provider.getName(),
                "Your provider account is now ACTIVE.\n\n"
                        + "Provider: " + provider.getName() + "\n"
                        + "Status: ACTIVE\n"
                        + "You can now use the provider portal.");
    }

    public void notifyExpiryWarningToAdmins(ServiceProvider provider) {
        if (approverRecipients.isEmpty()) return;
        send(approverRecipients.toArray(String[]::new),
                "Partner account expiring in 7 days: " + provider.getName(),
                "The following partner account will expire in 7 days and the manager will lose portal access.\n\n"
                        + "Partner: " + provider.getName() + "\n"
                        + "Expiry date: " + provider.getExpiryDate() + "\n"
                        + "Please renew by updating the expiry date in Management → Providers.");
    }

    public void notifyExpiredToAdmins(ServiceProvider provider) {
        if (approverRecipients.isEmpty()) return;
        send(approverRecipients.toArray(String[]::new),
                "Partner account has expired: " + provider.getName(),
                "The following partner account has expired and the manager can no longer log in.\n\n"
                        + "Partner: " + provider.getName() + "\n"
                        + "Expired on: " + provider.getExpiryDate() + "\n"
                        + "Please renew by updating the expiry date in Management → Providers.");
    }

    public void notifyRejected(ServiceProvider provider, String managerEmail, String reason) {
        String details = (reason == null || reason.isBlank()) ? "" : ("\nReason: " + reason.trim());
        send(managerEmail,
                "Provider submission rejected: " + provider.getName(),
                "Your provider submission was rejected.\n\n"
                        + "Provider: " + provider.getName() + "\n"
                        + "Status: INACTIVE" + details + "\n"
                        + "Please contact support/admin to resubmit with corrections.");
    }

    private void send(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            return;
        }
        send(new String[]{to.trim()}, subject, body);
    }

    private void send(String[] to, String subject, String body) {
        if (!enabled) {
            log.info("Email notifications disabled; skipped subject='{}' recipients={}", subject, Arrays.toString(to));
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailDispatchService.send(message);
            log.info("Email queued subject='{}' recipients={}", subject, Arrays.toString(to));
        } catch (Exception ex) {
            log.warn("Failed to send email subject='{}' recipients={} error={}",
                    subject, Arrays.toString(to), ex.getMessage());
        }
    }
}
