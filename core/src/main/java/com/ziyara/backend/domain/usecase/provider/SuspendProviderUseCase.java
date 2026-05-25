package com.ziyara.backend.domain.usecase.provider;

import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.domain.enums.ProviderStatus;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;

import java.util.Optional;
import java.util.UUID;

public class SuspendProviderUseCase {

    private final ServiceProviderRepository providerRepository;

    public SuspendProviderUseCase(ServiceProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    public Result execute(Input input) {
        Optional<ServiceProvider> providerOpt = providerRepository.findById(input.providerId());
        if (providerOpt.isEmpty()) {
            return Result.failure("Provider not found");
        }

        ServiceProvider provider = providerOpt.get();

        if (provider.getStatus() != ProviderStatus.ACTIVE) {
            return Result.failure("Only ACTIVE providers can be suspended. Current status: " + provider.getStatus());
        }

        provider.setStatus(ProviderStatus.INACTIVE);
        ServiceProvider saved = providerRepository.save(provider);
        return Result.success(saved);
    }

    public record Input(UUID providerId, String reason, UUID suspendedBy) {}

    public record Result(boolean success, ServiceProvider provider, String error) {
        public static Result success(ServiceProvider provider) {
            return new Result(true, provider, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
