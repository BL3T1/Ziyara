package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.CustomerSubscriptionResponse;
import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.domain.entity.CustomerSubscription;
import com.ziyara.backend.domain.entity.Plan;
import com.ziyara.backend.domain.entity.SubscriptionAddOn;
import com.ziyara.backend.domain.enums.AddOnStatus;
import com.ziyara.backend.domain.enums.SubscriptionStatus;
import com.ziyara.backend.domain.repository.CustomerSubscriptionRepository;
import com.ziyara.backend.domain.repository.PlanRepository;
import com.ziyara.backend.domain.repository.ProviderStaffRepository;
import com.ziyara.backend.domain.repository.SubscriptionAddOnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock PlanRepository planRepository;
    @Mock CustomerSubscriptionRepository subscriptionRepository;
    @Mock SubscriptionAddOnRepository addOnRepository;
    @Mock ProviderStaffRepository providerStaffRepository;
    @Mock JdbcTemplate jdbcTemplate;

    SubscriptionService service;

    UUID providerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(
                planRepository, subscriptionRepository, addOnRepository, providerStaffRepository, jdbcTemplate);
    }

    // ── resolveEffectiveSeatLimit ─────────────────────────────────────────────

    @Test
    void resolveEffectiveSeatLimit_noActiveSubscription_returnsFreePlanDefault() {
        when(subscriptionRepository.findActiveByProviderId(providerId)).thenReturn(Optional.empty());

        int limit = service.resolveEffectiveSeatLimit(providerId);

        assertThat(limit).isEqualTo(SubscriptionService.FREE_PLAN_DEFAULT_SEAT_LIMIT);
    }

    @Test
    void resolveEffectiveSeatLimit_activeSubscriptionNoAddOns_returnsBaseSeatLimit() {
        CustomerSubscription sub = subscription(10, SubscriptionStatus.ACTIVE);
        when(subscriptionRepository.findActiveByProviderId(providerId)).thenReturn(Optional.of(sub));
        when(addOnRepository.findActiveBySubscriptionId(sub.getId())).thenReturn(List.of());

        int limit = service.resolveEffectiveSeatLimit(providerId);

        assertThat(limit).isEqualTo(10);
    }

    @Test
    void resolveEffectiveSeatLimit_withActiveAddOn_returnsCombinedLimit() {
        CustomerSubscription sub = subscription(10, SubscriptionStatus.ACTIVE);
        SubscriptionAddOn addOn = addOn(5, AddOnStatus.ACTIVE);
        when(subscriptionRepository.findActiveByProviderId(providerId)).thenReturn(Optional.of(sub));
        when(addOnRepository.findActiveBySubscriptionId(sub.getId())).thenReturn(List.of(addOn));

        int limit = service.resolveEffectiveSeatLimit(providerId);

        assertThat(limit).isEqualTo(15);
    }

    @Test
    void resolveEffectiveSeatLimit_cancelledAddOnIgnored_returnsBaseLimit() {
        CustomerSubscription sub = subscription(10, SubscriptionStatus.ACTIVE);
        SubscriptionAddOn addOn = addOn(5, AddOnStatus.CANCELLED);
        when(subscriptionRepository.findActiveByProviderId(providerId)).thenReturn(Optional.of(sub));
        when(addOnRepository.findActiveBySubscriptionId(sub.getId())).thenReturn(List.of(addOn));

        int limit = service.resolveEffectiveSeatLimit(providerId);

        assertThat(limit).isEqualTo(10);
    }

    // ── getSubscription ───────────────────────────────────────────────────────

    @Test
    void getSubscription_noActiveSub_returnsFreeDefaults() {
        when(subscriptionRepository.findActiveByProviderId(providerId)).thenReturn(Optional.empty());
        when(providerStaffRepository.countByProviderId(providerId)).thenReturn(2L);

        CustomerSubscriptionResponse response = service.getSubscription(providerId);

        assertThat(response.getPlanCode()).isEqualTo("FREE");
        assertThat(response.getPlanName()).isEqualTo("Free");
        assertThat(response.getSeatLimit()).isEqualTo(SubscriptionService.FREE_PLAN_DEFAULT_SEAT_LIMIT);
        assertThat(response.getCurrentSeatCount()).isEqualTo(2);
        assertThat(response.getSeatsRemaining()).isEqualTo(4);
        assertThat(response.getAddOns()).isEmpty();
    }

    @Test
    void getSubscription_activeSub_includesPlanDetails() {
        CustomerSubscription sub = subscription(20, SubscriptionStatus.ACTIVE);
        Plan plan = plan("PRO", "Professional");
        plan.setId(sub.getPlanId());

        when(subscriptionRepository.findActiveByProviderId(providerId)).thenReturn(Optional.of(sub));
        when(addOnRepository.findActiveBySubscriptionId(sub.getId())).thenReturn(List.of());
        when(planRepository.findById(sub.getPlanId())).thenReturn(Optional.of(plan));
        when(providerStaffRepository.countByProviderId(providerId)).thenReturn(5L);

        CustomerSubscriptionResponse response = service.getSubscription(providerId);

        assertThat(response.getPlanCode()).isEqualTo("PRO");
        assertThat(response.getPlanName()).isEqualTo("Professional");
        assertThat(response.getSeatLimit()).isEqualTo(20);
        assertThat(response.getCurrentSeatCount()).isEqualTo(5);
        assertThat(response.getSeatsRemaining()).isEqualTo(15);
    }

    // ── assertCanAddUser ──────────────────────────────────────────────────────

    @Test
    void assertCanAddUser_underLimit_doesNotThrow() {
        when(providerStaffRepository.countByProviderId(providerId)).thenReturn(3L);
        when(subscriptionRepository.findActiveByProviderId(providerId)).thenReturn(Optional.empty());
        // FREE_PLAN_DEFAULT_SEAT_LIMIT = 6, count = 3 → ok

        service.assertCanAddUser(providerId);
        // no exception
    }

    @Test
    void assertCanAddUser_atLimit_throwsBusinessException() {
        CustomerSubscription sub = subscription(5, SubscriptionStatus.ACTIVE);
        when(providerStaffRepository.countByProviderId(providerId)).thenReturn(5L);
        when(subscriptionRepository.findActiveByProviderId(providerId)).thenReturn(Optional.of(sub));
        when(addOnRepository.findActiveBySubscriptionId(sub.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> service.assertCanAddUser(providerId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("maximum of 5");
    }

    // ── addSeatExpansion ──────────────────────────────────────────────────────

    @Test
    void addSeatExpansion_noActiveSub_throwsBusinessException() {
        when(subscriptionRepository.findActiveByProviderId(providerId)).thenReturn(Optional.empty());

        var request = new com.ziyara.backend.application.dto.request.AddSubscriptionAddOnRequest();
        request.setAddOnCode("EXTRA_SEATS");
        request.setDisplayName("Extra Seats");
        request.setExtraSeats(10);

        assertThatThrownBy(() -> service.addSeatExpansion(providerId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No active subscription");
    }

    @Test
    void addSeatExpansion_subscriptionNotUsable_throwsBusinessException() {
        CustomerSubscription sub = subscription(10, SubscriptionStatus.CANCELLED);
        when(subscriptionRepository.findActiveByProviderId(providerId)).thenReturn(Optional.of(sub));

        var request = new com.ziyara.backend.application.dto.request.AddSubscriptionAddOnRequest();
        request.setAddOnCode("EXTRA_SEATS");
        request.setDisplayName("Extra Seats");
        request.setExtraSeats(10);

        assertThatThrownBy(() -> service.addSeatExpansion(providerId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not in an active state");
    }

    // ── cancelAddOn ────────────────────────────────────────────────────────[...]

    @Test
    void cancelAddOn_addOnNotFound_throwsResourceNotFound() {
        UUID addOnId = UUID.randomUUID();
        when(addOnRepository.findById(addOnId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelAddOn(providerId, addOnId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Add-on not found");
    }

    @Test
    void cancelAddOn_wrongProvider_throwsBusinessException() {
        UUID addOnId = UUID.randomUUID();
        UUID otherProviderId = UUID.randomUUID();

        SubscriptionAddOn addOn = addOn(5, AddOnStatus.ACTIVE);
        addOn.setId(addOnId);

        CustomerSubscription sub = subscription(10, SubscriptionStatus.ACTIVE);
        sub.setProviderId(otherProviderId); // different provider
        addOn.setSubscriptionId(sub.getId());

        when(addOnRepository.findById(addOnId)).thenReturn(Optional.of(addOn));
        when(subscriptionRepository.findById(sub.getId())).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> service.cancelAddOn(providerId, addOnId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not belong to this provider");
    }

    // ── listPlans ─────────────────────────────────────────────────────────[...]

    @Test
    void listPlans_returnsActivePlans() {
        Plan p = plan("FREE", "Free");
        when(planRepository.findAllActive()).thenReturn(List.of(p));

        var result = service.listPlans();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("FREE");
    }

    // ── helpers ─────────────────────────────────────────────────────────[...]

    private CustomerSubscription subscription(int seatLimit, SubscriptionStatus status) {
        CustomerSubscription sub = new CustomerSubscription();
        sub.setId(UUID.randomUUID());
        sub.setProviderId(providerId);
        sub.setPlanId(UUID.randomUUID());
        sub.setSeatLimit(seatLimit);
        sub.setStatus(status);
        return sub;
    }

    private SubscriptionAddOn addOn(int extraSeats, AddOnStatus status) {
        SubscriptionAddOn a = new SubscriptionAddOn();
        a.setId(UUID.randomUUID());
        a.setAddOnCode("EXTRA_SEATS");
        a.setExtraSeats(extraSeats);
        a.setStatus(status);
        return a;
    }

    private Plan plan(String code, String name) {
        Plan p = new Plan();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        p.setName(name);
        p.setActive(true);
        p.setMaxUsers(6);
        return p;
    }
}
