package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.UpsertProviderSubscriptionRequest;
import com.ziyara.backend.application.dto.response.ProviderSubscriptionResponse;
import com.ziyara.backend.domain.entity.ProviderSubscription;
import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.domain.repository.ProviderSubscriptionRepository;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProviderSubscriptionService {

    private static final int FREE_STAFF_LIMIT = 10;

    private final ProviderSubscriptionRepository subscriptionRepository;
    private final ServiceProviderRepository serviceProviderRepository;

    @Transactional(readOnly = true)
    public ProviderSubscriptionResponse getByProviderId(UUID providerId) {
        return subscriptionRepository.findByProviderId(providerId)
                .map(this::toResponse)
                .orElseGet(() -> ProviderSubscriptionResponse.builder()
                        .providerId(providerId)
                        .plan("FREE")
                        .staffLimit(FREE_STAFF_LIMIT)
                        .build());
    }

    @Transactional(readOnly = true)
    public List<ProviderSubscriptionResponse> listAll() {
        Map<UUID, ProviderSubscription> subById = subscriptionRepository.findAll().stream()
                .collect(Collectors.toMap(ProviderSubscription::getProviderId, s -> s, (a, b) -> a));
        return serviceProviderRepository.findAll().stream()
                .map(p -> {
                    ProviderSubscription sub = subById.get(p.getId());
                    if (sub != null) return toResponse(sub, p.getName());
                    return ProviderSubscriptionResponse.builder()
                            .providerId(p.getId())
                            .providerName(p.getName())
                            .plan("FREE")
                            .staffLimit(FREE_STAFF_LIMIT)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ProviderSubscriptionResponse upsert(UUID providerId, UpsertProviderSubscriptionRequest request) {
        String plan = request.getPlan().trim().toUpperCase();
        if (!plan.equals("FREE") && !plan.equals("PRO")) {
            throw new IllegalArgumentException("Plan must be FREE or PRO");
        }
        int limit = "FREE".equals(plan) ? FREE_STAFF_LIMIT
                : (request.getStaffLimit() != null ? request.getStaffLimit() : FREE_STAFF_LIMIT);

        ProviderSubscription sub = subscriptionRepository.findByProviderId(providerId)
                .orElseGet(() -> {
                    ProviderSubscription s = new ProviderSubscription();
                    s.setProviderId(providerId);
                    return s;
                });
        sub.setPlan(plan);
        sub.setStaffLimit(limit);
        return toResponse(subscriptionRepository.save(sub));
    }

    @Transactional
    public void ensureFreeSubscription(UUID providerId) {
        if (subscriptionRepository.findByProviderId(providerId).isEmpty()) {
            ProviderSubscription sub = new ProviderSubscription();
            sub.setProviderId(providerId);
            sub.setPlan("FREE");
            sub.setStaffLimit(FREE_STAFF_LIMIT);
            subscriptionRepository.save(sub);
        }
    }

    private ProviderSubscriptionResponse toResponse(ProviderSubscription s) {
        return toResponse(s, null);
    }

    private ProviderSubscriptionResponse toResponse(ProviderSubscription s, String providerName) {
        return ProviderSubscriptionResponse.builder()
                .id(s.getId())
                .providerId(s.getProviderId())
                .providerName(providerName)
                .plan(s.getPlan())
                .staffLimit(s.getStaffLimit())
                .build();
    }
}
