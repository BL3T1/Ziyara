package com.ziyara.backend.domain.usecase.provider;

import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.domain.enums.ProviderStatus;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class ApproveProviderUseCase {

    private final ServiceProviderRepository providerRepository;

    public ApproveProviderUseCase(ServiceProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    public Result execute(Input input) {
        Optional<ServiceProvider> providerOpt = providerRepository.findById(input.providerId());
        if (providerOpt.isEmpty()) {
            return Result.failure("Provider not found");
        }

        ServiceProvider provider = providerOpt.get();

        if (provider.getStatus() != ProviderStatus.PENDING_APPROVAL) {
            return Result.failure("Only PENDING_APPROVAL providers can be approved. Current status: "
                    + provider.getStatus());
        }

        if (input.approve()) {
            provider.setStatus(ProviderStatus.ACTIVE);
            provider.setVerified(true);
            provider.setApprovedBy(input.reviewedBy());
            provider.setApprovedAt(LocalDateTime.now());
        } else {
            provider.setStatus(ProviderStatus.INACTIVE);
        }

        ServiceProvider saved = providerRepository.save(provider);
        return Result.success(saved);
    }

    public record Input(UUID providerId, boolean approve, UUID reviewedBy) {}

    public record Result(boolean success, ServiceProvider provider, String error) {
        public static Result success(ServiceProvider provider) {
            return new Result(true, provider, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
