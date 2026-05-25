package com.ziyara.backend.domain.usecase.subscription;

import com.ziyara.backend.domain.entity.CustomerSubscription;
import com.ziyara.backend.domain.entity.Plan;
import com.ziyara.backend.domain.enums.SubscriptionStatus;
import com.ziyara.backend.domain.repository.CustomerSubscriptionRepository;
import com.ziyara.backend.domain.repository.PlanRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class ActivateSubscriptionUseCase {

    private final CustomerSubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;

    public ActivateSubscriptionUseCase(CustomerSubscriptionRepository subscriptionRepository,
                                       PlanRepository planRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
    }

    public Result execute(Input input) {
        Optional<Plan> planOpt = planRepository.findById(input.planId());
        if (planOpt.isEmpty()) {
            return Result.failure("Plan not found");
        }

        Plan plan = planOpt.get();

        if (!plan.isActive()) {
            return Result.failure("Plan is not available for subscription");
        }

        // Reject if there's already an active/trial subscription
        Optional<CustomerSubscription> existing =
                subscriptionRepository.findActiveByProviderId(input.providerId());
        if (existing.isPresent()) {
            return Result.failure("Provider already has an active subscription. Cancel the existing one first");
        }

        int seatLimit = plan.effectiveMaxUsers() == Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : plan.getMaxUsers();

        CustomerSubscription subscription = new CustomerSubscription();
        subscription.setProviderId(input.providerId());
        subscription.setPlanId(input.planId());
        subscription.setSeatLimit(seatLimit);
        subscription.setCurrentPeriodStart(input.periodStart());
        subscription.setCurrentPeriodEnd(input.periodEnd());
        subscription.setTrialEndsAt(input.trialEndsAt());
        subscription.setStatus(input.trialEndsAt() != null ? SubscriptionStatus.TRIAL : SubscriptionStatus.ACTIVE);
        subscription.setCreatedAt(Instant.now());
        subscription.setUpdatedAt(Instant.now());

        CustomerSubscription saved = subscriptionRepository.save(subscription);
        return Result.success(saved);
    }

    public record Input(
            UUID providerId,
            UUID planId,
            Instant periodStart,
            Instant periodEnd,
            Instant trialEndsAt
    ) {}

    public record Result(boolean success, CustomerSubscription subscription, String error) {
        public static Result success(CustomerSubscription subscription) {
            return new Result(true, subscription, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
