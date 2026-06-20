package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.UpsertProviderSubscriptionRequest;
import com.ziyara.backend.application.dto.response.ProviderSubscriptionResponse;
import com.ziyara.backend.domain.entity.ProviderSubscription;
import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.domain.repository.ProviderSubscriptionRepository;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderSubscriptionServiceTest {

    @Mock ProviderSubscriptionRepository subscriptionRepository;
    @Mock ServiceProviderRepository serviceProviderRepository;

    ProviderSubscriptionService service;

    @BeforeEach
    void setUp() {
        service = new ProviderSubscriptionService(subscriptionRepository, serviceProviderRepository);
    }

    // ── getByProviderId ───────────────────────────────────────────────────────

    @Test
    void getByProviderId_noSubscription_returnsFreePlanDefaults() {
        UUID providerId = UUID.randomUUID();
        when(subscriptionRepository.findByProviderId(providerId)).thenReturn(Optional.empty());

        ProviderSubscriptionResponse result = service.getByProviderId(providerId);

        assertThat(result.getPlan()).isEqualTo("FREE");
        assertThat(result.getStaffLimit()).isEqualTo(10);
    }

    @Test
    void getByProviderId_existingSubscription_returnsStoredPlan() {
        UUID providerId = UUID.randomUUID();
        ProviderSubscription sub = new ProviderSubscription();
        sub.setProviderId(providerId);
        sub.setPlan("PRO");
        sub.setStaffLimit(50);
        when(subscriptionRepository.findByProviderId(providerId)).thenReturn(Optional.of(sub));

        ProviderSubscriptionResponse result = service.getByProviderId(providerId);

        assertThat(result.getPlan()).isEqualTo("PRO");
        assertThat(result.getStaffLimit()).isEqualTo(50);
    }

    // ── listAll ───────────────────────────────────────────────────────────────

    @Test
    void listAll_providerWithNoSubscription_showsFreePlanDefaults() {
        UUID providerId = UUID.randomUUID();
        ServiceProvider provider = new ServiceProvider();
        provider.setId(providerId);
        provider.setName("Ziyara Hotel");

        when(serviceProviderRepository.findAll()).thenReturn(List.of(provider));
        when(subscriptionRepository.findAll()).thenReturn(List.of());

        List<ProviderSubscriptionResponse> result = service.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPlan()).isEqualTo("FREE");
        assertThat(result.get(0).getProviderName()).isEqualTo("Ziyara Hotel");
    }

    // ── upsert ────────────────────────────────────────────────────────────────

    @Test
    void upsert_invalidPlan_throwsIllegalArgument() {
        UUID providerId = UUID.randomUUID();
        UpsertProviderSubscriptionRequest request = new UpsertProviderSubscriptionRequest();
        request.setPlan("ENTERPRISE");

        assertThatThrownBy(() -> service.upsert(providerId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plan must be FREE or PRO");
    }

    @Test
    void upsert_freePlan_usesDefaultStaffLimit() {
        UUID providerId = UUID.randomUUID();
        when(subscriptionRepository.findByProviderId(providerId)).thenReturn(Optional.empty());
        ProviderSubscription saved = new ProviderSubscription();
        saved.setProviderId(providerId);
        saved.setPlan("FREE");
        saved.setStaffLimit(10);
        when(subscriptionRepository.save(any())).thenReturn(saved);

        UpsertProviderSubscriptionRequest request = new UpsertProviderSubscriptionRequest();
        request.setPlan("free");

        ProviderSubscriptionResponse result = service.upsert(providerId, request);

        assertThat(result.getPlan()).isEqualTo("FREE");
        assertThat(result.getStaffLimit()).isEqualTo(10);
    }

    @Test
    void upsert_proPlanWithExplicitLimit_usesProvidedLimit() {
        UUID providerId = UUID.randomUUID();
        when(subscriptionRepository.findByProviderId(providerId)).thenReturn(Optional.empty());
        ProviderSubscription saved = new ProviderSubscription();
        saved.setProviderId(providerId);
        saved.setPlan("PRO");
        saved.setStaffLimit(100);
        when(subscriptionRepository.save(any())).thenReturn(saved);

        UpsertProviderSubscriptionRequest request = new UpsertProviderSubscriptionRequest();
        request.setPlan("PRO");
        request.setStaffLimit(100);

        ProviderSubscriptionResponse result = service.upsert(providerId, request);

        ArgumentCaptor<ProviderSubscription> captor = ArgumentCaptor.forClass(ProviderSubscription.class);
        org.mockito.Mockito.verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getStaffLimit()).isEqualTo(100);
    }
}
