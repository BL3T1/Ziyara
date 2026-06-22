package com.ziyara.backend.domain.usecase.subscription;

import com.ziyara.backend.domain.entity.CustomerSubscription;
import com.ziyara.backend.domain.enums.SubscriptionStatus;
import com.ziyara.backend.domain.repository.CustomerSubscriptionRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class CancelSubscriptionUseCase {

    private final CustomerSubscriptionRepository subscriptionRepository;

    public CancelSubscriptionUseCase(CustomerSubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public Result execute(Input input) {
        Optional<CustomerSubscription> subOpt = subscriptionRepository.findById(input.subscriptionId());
        if (subOpt.isEmpty()) {
            return Result.failure("Subscription not found");
        }

        CustomerSubscription subscription = subOpt.get();

        if (!subscription.isUsable()) {
            return Result.failure("Subscription is not active or on trial. Current status: "
                    + subscription.getStatus());
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(Instant.now());
        subscription.setUpdatedAt(Instant.now());

        CustomerSubscription saved = subscriptionRepository.save(subscription);
        return Result.success(saved);
    }

    public record Input(UUID subscriptionId, UUID cancelledBy) {}

    public record Result(boolean success, CustomerSubscription subscription, String error) {
        public static Result success(CustomerSubscription subscription) {
            return new Result(true, subscription, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
