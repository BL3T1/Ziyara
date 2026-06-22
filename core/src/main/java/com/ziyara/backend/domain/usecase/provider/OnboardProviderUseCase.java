package com.ziyara.backend.domain.usecase.provider;

import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.domain.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

public class OnboardProviderUseCase {

    private final ServiceProviderRepository providerRepository;
    private final UserRepository userRepository;

    public OnboardProviderUseCase(ServiceProviderRepository providerRepository, UserRepository userRepository) {
        this.providerRepository = providerRepository;
        this.userRepository = userRepository;
    }

    public Result execute(Input input) {
        if (!userRepository.existsById(input.userId())) {
            return Result.failure("User not found");
        }

        // One provider account per user
        Optional<ServiceProvider> existing = providerRepository.findByUserId(input.userId());
        if (existing.isPresent()) {
            return Result.failure("A provider account already exists for this user");
        }

        if (providerRepository.existsByName(input.name())) {
            return Result.failure("A provider with this name already exists");
        }

        ServiceProvider provider = new ServiceProvider();
        provider.setUserId(input.userId());
        provider.setName(input.name());
        provider.setNameAr(input.nameAr());
        provider.setType(input.type());
        provider.setPhone(input.phone());
        provider.setEmail(input.email());
        provider.setWebsite(input.website());
        provider.setAddress(input.address());
        provider.setDescription(input.description());
        provider.setDescriptionAr(input.descriptionAr());
        provider.setRegistrationNumber(input.registrationNumber());
        provider.setTaxNumber(input.taxNumber());

        ServiceProvider saved = providerRepository.save(provider);
        return Result.success(saved);
    }

    public record Input(
            UUID userId,
            String name,
            String nameAr,
            String type,
            String phone,
            String email,
            String website,
            String address,
            String description,
            String descriptionAr,
            String registrationNumber,
            String taxNumber
    ) {}

    public record Result(boolean success, ServiceProvider provider, String error) {
        public static Result success(ServiceProvider provider) {
            return new Result(true, provider, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
