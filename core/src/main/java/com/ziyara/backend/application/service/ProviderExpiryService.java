package com.ziyara.backend.application.service;

import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.domain.enums.NotificationType;
import com.ziyara.backend.domain.enums.ProviderStatus;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.modules.notification.api.StaffNotificationCommandPublisher;
import com.ziyara.backend.application.dto.StaffNotificationEvent;
import com.ziyara.backend.modules.provider.api.ProviderExpiryApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderExpiryService implements ProviderExpiryApi {

    private static final int WARNING_DAYS = 7;

    private final ServiceProviderRepository serviceProviderRepository;
    private final StaffNotificationCommandPublisher staffNotificationCommandPublisher;
    private final ProviderWorkflowEmailService providerWorkflowEmailService;

    @Transactional
    @Override
    public void notifyExpiringProviders() {
        LocalDate today = LocalDate.now();

        List<ServiceProvider> expiredToday = serviceProviderRepository.findByExpiryDate(today)
                .stream()
                .filter(p -> p.getStatus() == ProviderStatus.ACTIVE)
                .toList();

        List<ServiceProvider> expiringInWarningDays = serviceProviderRepository.findByExpiryDate(today.plusDays(WARNING_DAYS))
                .stream()
                .filter(p -> p.getStatus() == ProviderStatus.ACTIVE)
                .toList();

        for (ServiceProvider provider : expiredToday) {
            publishStaffEvent(provider, NotificationType.PROVIDER_EXPIRED,
                    "Partner account has expired",
                    "Partner \"" + provider.getName() + "\" expired on " + provider.getExpiryDate() + ". Manager login is now blocked.");
            providerWorkflowEmailService.notifyExpiredToAdmins(provider);
        }

        for (ServiceProvider provider : expiringInWarningDays) {
            publishStaffEvent(provider, NotificationType.PROVIDER_EXPIRY_WARNING,
                    "Partner expiring in " + WARNING_DAYS + " days",
                    "Partner \"" + provider.getName() + "\" will expire on " + provider.getExpiryDate() + ". Please renew.");
            providerWorkflowEmailService.notifyExpiryWarningToAdmins(provider);
        }

        log.info("[ProviderExpiry] Notified: {} expired, {} expiring in {} days",
                expiredToday.size(), expiringInWarningDays.size(), WARNING_DAYS);
    }

    private void publishStaffEvent(ServiceProvider provider, NotificationType type, String title, String message) {
        staffNotificationCommandPublisher.publishAfterCommit(StaffNotificationEvent.builder()
                .eventId(UUID.randomUUID())
                .notificationType(type.name())
                .title(title)
                .message(message)
                .notifyRoles(List.of("SUPER_ADMIN"))
                .metadata("{\"providerId\":\"" + provider.getId() + "\"}")
                .build());
    }
}
