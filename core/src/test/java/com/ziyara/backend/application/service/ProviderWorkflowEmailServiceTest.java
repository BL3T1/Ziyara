package com.ziyara.backend.application.service;

import com.ziyara.backend.domain.entity.ServiceProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderWorkflowEmailServiceTest {

    @Mock MailDispatchService mailDispatchService;

    private ProviderWorkflowEmailService build(boolean enabled, String approversCsv) {
        return new ProviderWorkflowEmailService(
                mailDispatchService, enabled,
                "no-reply@ziyara.local",
                approversCsv);
    }

    private ServiceProvider provider(String name) {
        ServiceProvider p = new ServiceProvider();
        p.setName(name);
        return p;
    }

    // ── disabled ──────────────────────────────────────────────────────────────

    @Test
    void notifySubmittedForApproval_disabled_neverSends() {
        ProviderWorkflowEmailService svc = build(false, "admin@example.com");

        svc.notifySubmittedForApproval(provider("Acme"), "manager@example.com");

        verify(mailDispatchService, never()).send(any());
    }

    @Test
    void notifyActivated_disabled_neverSends() {
        ProviderWorkflowEmailService svc = build(false, "");

        svc.notifyActivated(provider("Acme"), "manager@example.com");

        verify(mailDispatchService, never()).send(any());
    }

    @Test
    void notifyRejected_disabled_neverSends() {
        ProviderWorkflowEmailService svc = build(false, "");

        svc.notifyRejected(provider("Acme"), "manager@example.com", "Bad photos");

        verify(mailDispatchService, never()).send(any());
    }

    // ── enabled, no approvers ─────────────────────────────────────────────────

    @Test
    void notifySubmittedForApproval_enabledNoApprovers_sendsOnlyToManager() {
        ProviderWorkflowEmailService svc = build(true, "");

        svc.notifySubmittedForApproval(provider("Grand Hotel"), "manager@example.com");

        verify(mailDispatchService, times(1)).send(any());
    }

    @Test
    void notifyActivated_enabled_sendsToManager() {
        ProviderWorkflowEmailService svc = build(true, "");

        svc.notifyActivated(provider("Grand Hotel"), "manager@example.com");

        verify(mailDispatchService, times(1)).send(any());
    }

    @Test
    void notifyRejected_enabled_sendsToManager() {
        ProviderWorkflowEmailService svc = build(true, "");

        svc.notifyRejected(provider("Grand Hotel"), "manager@example.com", "Blurry logo");

        verify(mailDispatchService, times(1)).send(any());
    }

    // ── enabled, with approvers ───────────────────────────────────────────────

    @Test
    void notifySubmittedForApproval_withApprovers_sendsTwice() {
        ProviderWorkflowEmailService svc = build(true, "admin1@example.com,admin2@example.com");

        svc.notifySubmittedForApproval(provider("Grand Hotel"), "manager@example.com");

        // once to manager, once to approvers group
        verify(mailDispatchService, times(2)).send(any());
    }

    // ── blank / null recipient ────────────────────────────────────────────────

    @Test
    void notifyActivated_blankManagerEmail_skipsQuietly() {
        ProviderWorkflowEmailService svc = build(true, "");

        svc.notifyActivated(provider("Grand Hotel"), "  ");

        verify(mailDispatchService, never()).send(any());
    }

    @Test
    void notifyActivated_nullManagerEmail_skipsQuietly() {
        ProviderWorkflowEmailService svc = build(true, "");

        svc.notifyActivated(provider("Grand Hotel"), null);

        verify(mailDispatchService, never()).send(any());
    }
}
