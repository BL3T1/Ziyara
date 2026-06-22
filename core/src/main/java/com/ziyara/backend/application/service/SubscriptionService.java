package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.ActivateSubscriptionRequest;
import com.ziyara.backend.application.dto.request.AddSubscriptionAddOnRequest;
import com.ziyara.backend.application.dto.response.CustomerSubscriptionResponse;
import com.ziyara.backend.modules.subscription.api.SubscriptionServiceApi;
import com.ziyara.backend.application.dto.response.PlanResponse;
import com.ziyara.backend.application.dto.response.SubscriptionAddOnResponse;
import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.domain.entity.CustomerSubscription;
import com.ziyara.backend.domain.entity.Plan;
import com.ziyara.backend.domain.entity.SubscriptionAddOn;
import com.ziyara.backend.domain.enums.AddOnStatus;
import com.ziyara.backend.domain.enums.SubscriptionStatus;
import com.ziyara.backend.domain.usecase.subscription.ActivateSubscriptionUseCase;
import com.ziyara.backend.domain.repository.CustomerSubscriptionRepository;
import com.ziyara.backend.domain.repository.PlanRepository;
import com.ziyara.backend.domain.repository.ProviderStaffRepository;
import com.ziyara.backend.domain.repository.SubscriptionAddOnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service: SubscriptionService
 *
 * Enforces the per-provider seat limit:
 *   - FREE plan (default)   → 6 portal users
 *   - STARTER               → 15
 *   - PROFESSIONAL          → 50  (+ overage billing)
 *   - ENTERPRISE            → unlimited (-1)
 *   - Active EXTRA_SEATS add-ons extend the ceiling further.
 *
 * If a provider has no subscription row the FREE plan limits apply automatically.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService implements SubscriptionServiceApi {

    /** Default FREE-plan seat ceiling applied when no subscription row exists. */
    public static final int FREE_PLAN_DEFAULT_SEAT_LIMIT = 6;

    private final PlanRepository planRepository;
    private final CustomerSubscriptionRepository subscriptionRepository;
    private final SubscriptionAddOnRepository addOnRepository;
    private final ProviderStaffRepository providerStaffRepository;
    private final JdbcTemplate jdbcTemplate;

    // -------------------------------------------------------------------------
    // Query
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PlanResponse> listPlans() {
        return planRepository.findAllActive().stream()
                .map(this::mapPlan)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CustomerSubscriptionResponse getSubscription(UUID providerId) {
        CustomerSubscription sub = subscriptionRepository
                .findActiveByProviderId(providerId)
                .orElse(null);

        if (sub == null) {
            return buildFreePlanResponse(providerId);
        }

        List<SubscriptionAddOn> addOns = addOnRepository.findActiveBySubscriptionId(sub.getId());
        sub.setAddOns(addOns);

        Plan plan = planRepository.findById(sub.getPlanId()).orElse(null);
        int currentCount = (int) providerStaffRepository.countByProviderId(providerId);
        int effective = sub.effectiveSeatLimit();

        return CustomerSubscriptionResponse.builder()
                .id(sub.getId())
                .providerId(providerId)
                .planCode(plan != null ? plan.getCode() : "UNKNOWN")
                .planName(plan != null ? plan.getName() : "Unknown")
                .status(sub.getStatus().name())
                .seatLimit(sub.getSeatLimit())
                .effectiveSeatLimit(effective)
                .currentSeatCount(currentCount)
                .seatsRemaining(Math.max(0, effective - currentCount))
                .currentPeriodStart(sub.getCurrentPeriodStart())
                .currentPeriodEnd(sub.getCurrentPeriodEnd())
                .addOns(addOns.stream().map(this::mapAddOn).collect(Collectors.toList()))
                .build();
    }

    // -------------------------------------------------------------------------
    // Seat-limit enforcement (called by PortalStaffService before provisioning)
    // -------------------------------------------------------------------------

    /**
     * Asserts that the provider can provision one more portal user.
     * Throws {@link BusinessException} with code SEAT_LIMIT_EXCEEDED if not.
     */
    @Transactional(readOnly = true)
    public void assertCanAddUser(UUID providerId) {
        int currentCount = (int) providerStaffRepository.countByProviderId(providerId);
        int effectiveLimit = resolveEffectiveSeatLimit(providerId);

        if (effectiveLimit != -1 && currentCount >= effectiveLimit) {
            String msg = String.format(
                    "Your current plan allows a maximum of %d portal users. "
                    + "You have %d active users. "
                    + "Upgrade your subscription or add a seat expansion to provision more users.",
                    effectiveLimit, currentCount);
            throw new BusinessException(msg);
        }
    }

    /**
     * Returns the effective seat ceiling for a provider.
     * -1 means unlimited (ENTERPRISE plan).
     */
    @Transactional(readOnly = true)
    public int resolveEffectiveSeatLimit(UUID providerId) {
        return subscriptionRepository.findActiveByProviderId(providerId)
                .map(sub -> {
                    List<SubscriptionAddOn> addOns =
                            addOnRepository.findActiveBySubscriptionId(sub.getId());
                    sub.setAddOns(addOns);
                    return sub.effectiveSeatLimit();
                })
                .orElse(FREE_PLAN_DEFAULT_SEAT_LIMIT);
    }

    // -------------------------------------------------------------------------
    // Mutations (admin / billing webhook surface)
    // -------------------------------------------------------------------------

    @Transactional
    public CustomerSubscriptionResponse activateSubscription(UUID providerId,
                                                             ActivateSubscriptionRequest request) {
        Plan plan = planRepository.findByCode(request.getPlanCode().toUpperCase())
                .filter(Plan::isActive)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Plan not found: " + request.getPlanCode()));

        // Cancel any existing active subscription before creating the new one
        subscriptionRepository.findActiveByProviderId(providerId).ifPresent(existing -> {
            existing.setStatus(SubscriptionStatus.CANCELLED);
            existing.setCancelledAt(Instant.now());
            subscriptionRepository.save(existing);
            log.info("Cancelled previous subscription {} for provider {}", existing.getId(), providerId);
        });

        var result = new ActivateSubscriptionUseCase(subscriptionRepository, planRepository).execute(
                new ActivateSubscriptionUseCase.Input(
                        providerId, plan.getId(), Instant.now(), null, null));
        if (!result.success()) throw new BusinessException(result.error());

        log.info("Activated plan {} for provider {}, seat limit {}",
                plan.getCode(), providerId, result.subscription().getSeatLimit());

        // Record a billing event for paid plans so the financial ledger stays complete
        if (plan.getMonthlyPrice() != null
                && plan.getMonthlyPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
            try {
                jdbcTemplate.update(
                        "INSERT INTO sub_billing_records " +
                        "(id, subscription_id, provider_id, plan_code, amount, currency, billing_cycle, status, created_at) " +
                        "VALUES (gen_random_uuid(), ?, ?, ?, ?, 'USD', 'MONTHLY', 'RECORDED', NOW())",
                        result.subscription().getId(), providerId,
                        plan.getCode(), plan.getMonthlyPrice());
            } catch (Exception ex) {
                log.warn("Could not record billing event for subscription {}: {}", result.subscription().getId(), ex.getMessage());
            }
        }

        return getSubscription(providerId);
    }

    @Transactional
    public CustomerSubscriptionResponse addSeatExpansion(UUID providerId,
                                                          AddSubscriptionAddOnRequest request) {
        CustomerSubscription sub = subscriptionRepository.findActiveByProviderId(providerId)
                .orElseThrow(() -> new BusinessException(
                        "No active subscription found. Activate a plan before adding seat expansions."));

        if (!sub.isUsable()) {
            throw new BusinessException("Subscription is not in an active state.");
        }

        SubscriptionAddOn addOn = new SubscriptionAddOn();
        addOn.setSubscriptionId(sub.getId());
        addOn.setAddOnCode(request.getAddOnCode().toUpperCase());
        addOn.setDisplayName(request.getDisplayName());
        addOn.setExtraSeats(request.getExtraSeats());
        addOn.setPrice(request.getPrice());
        addOn.setStatus(AddOnStatus.ACTIVE);
        addOn.setActivatedAt(Instant.now());
        addOnRepository.save(addOn);

        log.info("Added seat expansion '{}' (+{} seats) to subscription {} for provider {}",
                addOn.getAddOnCode(), addOn.getExtraSeats(), sub.getId(), providerId);

        return getSubscription(providerId);
    }

    @Transactional
    public CustomerSubscriptionResponse cancelAddOn(UUID providerId, UUID addOnId) {
        SubscriptionAddOn addOn = addOnRepository.findById(addOnId)
                .orElseThrow(() -> new ResourceNotFoundException("Add-on not found"));

        CustomerSubscription sub = subscriptionRepository.findById(addOn.getSubscriptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        if (!sub.getProviderId().equals(providerId)) {
            throw new BusinessException("Add-on does not belong to this provider");
        }

        addOn.setStatus(AddOnStatus.CANCELLED);
        addOnRepository.save(addOn);

        log.info("Cancelled add-on {} for provider {}", addOnId, providerId);
        return getSubscription(providerId);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private CustomerSubscriptionResponse buildFreePlanResponse(UUID providerId) {
        int currentCount = (int) providerStaffRepository.countByProviderId(providerId);
        return CustomerSubscriptionResponse.builder()
                .id(null)
                .providerId(providerId)
                .planCode("FREE")
                .planName("Free")
                .status(SubscriptionStatus.ACTIVE.name())
                .seatLimit(FREE_PLAN_DEFAULT_SEAT_LIMIT)
                .effectiveSeatLimit(FREE_PLAN_DEFAULT_SEAT_LIMIT)
                .currentSeatCount(currentCount)
                .seatsRemaining(Math.max(0, FREE_PLAN_DEFAULT_SEAT_LIMIT - currentCount))
                .currentPeriodStart(null)
                .currentPeriodEnd(null)
                .addOns(List.of())
                .build();
    }

    private PlanResponse mapPlan(Plan p) {
        return PlanResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(p.getName())
                .description(p.getDescription())
                .maxUsers(p.getMaxUsers())
                .monthlyPrice(p.getMonthlyPrice())
                .currency(p.getCurrency())
                .allowsOverage(p.isAllowsOverage())
                .overagePricePerUser(p.getOveragePricePerUser())
                .build();
    }

    private SubscriptionAddOnResponse mapAddOn(SubscriptionAddOn a) {
        return SubscriptionAddOnResponse.builder()
                .id(a.getId())
                .addOnCode(a.getAddOnCode())
                .displayName(a.getDisplayName())
                .extraSeats(a.getExtraSeats())
                .price(a.getPrice())
                .status(a.getStatus() != null ? a.getStatus().name() : null)
                .activatedAt(a.getActivatedAt())
                .expiresAt(a.getExpiresAt())
                .build();
    }
}
