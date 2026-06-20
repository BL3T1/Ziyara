package com.ziyara.backend.domain.usecase.service;

import com.ziyara.backend.domain.entity.Service;
import com.ziyara.backend.domain.repository.ServiceRepository;

import java.util.Optional;
import java.util.UUID;

public class UpdateServiceAvailabilityUseCase {

    private final ServiceRepository serviceRepository;

    public UpdateServiceAvailabilityUseCase(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public Result execute(Input input) {
        Optional<Service> serviceOpt = serviceRepository.findById(input.serviceId());
        if (serviceOpt.isEmpty()) {
            return Result.failure("Service not found");
        }

        Service service = serviceOpt.get();

        if (input.availableRooms() < 0) {
            return Result.failure("Available rooms cannot be negative");
        }

        if (service.getTotalRooms() != null && input.availableRooms() > service.getTotalRooms()) {
            return Result.failure("Available rooms (" + input.availableRooms()
                    + ") cannot exceed total rooms (" + service.getTotalRooms() + ")");
        }

        service.setAvailableRooms(input.availableRooms());
        Service saved = serviceRepository.save(service);
        return Result.success(saved);
    }

    public record Input(UUID serviceId, int availableRooms) {}

    public record Result(boolean success, Service service, String error) {
        public static Result success(Service service) {
            return new Result(true, service, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
