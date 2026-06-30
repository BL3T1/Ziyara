package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreatePortalSupportRequest;
import com.ziyara.backend.application.dto.response.PortalSupportRequestResponse;
import com.ziyara.backend.domain.entity.PortalSupportRequest;
import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.domain.repository.PortalSupportRequestRepository;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.modules.notification.api.StaffNotificationCommandPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortalSupportRequestServiceTest {

    @Mock PortalSupportRequestRepository repository;
    @Mock ServiceProviderRepository serviceProviderRepository;
    @Mock StaffNotificationCommandPublisher staffNotificationCommandPublisher;

    PortalSupportRequestService service;

    UUID providerId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PortalSupportRequestService(repository, serviceProviderRepository, staffNotificationCommandPublisher);
    }

    // â”€â”€ listForProvider â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void listForProvider_returnsProviderRequests() {
        PortalSupportRequest req = request(providerId, "Login issue");
        when(repository.findByProviderId(providerId)).thenReturn(List.of(req));

        List<PortalSupportRequestResponse> result = service.listForProvider(providerId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSubject()).isEqualTo("Login issue");
    }

    @Test
    void listForProvider_noRequests_returnsEmpty() {
        when(repository.findByProviderId(providerId)).thenReturn(List.of());

        assertThat(service.listForProvider(providerId)).isEmpty();
    }

    // â”€â”€ listAll â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void listAll_includesProviderName() {
        PortalSupportRequest req = request(providerId, "Billing question");
        ServiceProvider provider = new ServiceProvider();
        provider.setId(providerId);
        provider.setName("Grand Hotel");

        when(repository.findAllOrderedByCreatedAtDesc()).thenReturn(List.of(req));
        when(serviceProviderRepository.findById(providerId)).thenReturn(Optional.of(provider));

        List<PortalSupportRequestResponse> result = service.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProviderName()).isEqualTo("Grand Hotel");
    }

    @Test
    void listAll_providerNotFound_usesNullName() {
        PortalSupportRequest req = request(providerId, "Technical issue");
        when(repository.findAllOrderedByCreatedAtDesc()).thenReturn(List.of(req));
        when(serviceProviderRepository.findById(providerId)).thenReturn(Optional.empty());

        List<PortalSupportRequestResponse> result = service.listAll();

        assertThat(result.get(0).getProviderName()).isNull();
    }

    // â”€â”€ create â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void create_trimsSubjectAndBody_andPublishesNotification() {
        CreatePortalSupportRequest createRequest = new CreatePortalSupportRequest();
        createRequest.setSubject("  Payment delay  ");
        createRequest.setBody("  Details here  ");

        PortalSupportRequest saved = request(providerId, "Payment delay");
        when(repository.save(any())).thenReturn(saved);

        service.create(providerId, userId, createRequest);

        ArgumentCaptor<PortalSupportRequest> captor = ArgumentCaptor.forClass(PortalSupportRequest.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo("Payment delay");
        assertThat(captor.getValue().getBody()).isEqualTo("Details here");
        verify(staffNotificationCommandPublisher).publishAfterCommit(any());
    }

    private PortalSupportRequest request(UUID providerId, String subject) {
        PortalSupportRequest r = new PortalSupportRequest();
        r.setId(UUID.randomUUID());
        r.setProviderId(providerId);
        r.setUserId(userId);
        r.setSubject(subject);
        r.setBody("Details");
        r.setCreatedAt(Instant.now());
        return r;
    }
}

