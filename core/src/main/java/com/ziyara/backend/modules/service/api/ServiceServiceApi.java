package com.ziyara.backend.modules.service.api;

import com.ziyara.backend.application.dto.request.CreateServiceRequest;
import com.ziyara.backend.application.dto.request.UpdateServiceRequest;
import com.ziyara.backend.application.dto.response.ServiceAvailabilityResponse;
import com.ziyara.backend.application.dto.response.ServiceResponse;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service-catalog module API.
 * Consumers (portal facade, admin) must depend only on this interface.
 */
public interface ServiceServiceApi {

    ServiceResponse create(CreateServiceRequest request);

    ServiceResponse update(UUID id, UpdateServiceRequest request);

    void deleteById(UUID id);

    ServiceResponse approve(UUID id);

    ServiceResponse suspend(UUID id);

    ServiceAvailabilityResponse checkAvailability(UUID serviceId, LocalDate date, int nights);
}
