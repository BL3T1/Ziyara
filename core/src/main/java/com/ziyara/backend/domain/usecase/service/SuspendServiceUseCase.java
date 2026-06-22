package com.ziyara.backend.domain.usecase.service;

import com.ziyara.backend.domain.entity.Service;
import com.ziyara.backend.domain.enums.ServiceStatus;
import com.ziyara.backend.domain.repository.ServiceRepository;

import java.util.Optional;
import java.util.UUID;

public class SuspendServiceUseCase {

    private final ServiceRepository serviceRepository;

    public SuspendServiceUseCase(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public Result execute(Input input) {
        Optional<Service> serviceOpt = serviceRepository.findById(input.serviceId());
        if (serviceOpt.isEmpty()) {
            return Result.failure("Service not found");
        }

        Service service = serviceOpt.get();

        if (service.getStatus() != ServiceStatus.ACTIVE) {
            return Result.failure("Only ACTIVE services can be suspended. Current status: " + service.getStatus());
        }

        service.suspend();
        Service saved = serviceRepository.save(service);
        return Result.success(saved);
    }

    public record Input(UUID serviceId, String reason, UUID suspendedBy) {}

    public record Result(boolean success, Service service, String error) {
        public static Result success(Service service) {
            return new Result(true, service, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
