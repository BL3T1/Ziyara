package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateServiceRequest;
import com.ziyara.backend.application.dto.request.UpdateServiceRequest;
import com.ziyara.backend.application.dto.response.ServiceAvailabilityResponse;
import com.ziyara.backend.application.dto.response.ServiceResponse;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.domain.entity.Booking;
import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.domain.enums.BookingStatus;
import com.ziyara.backend.domain.enums.ServiceType;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.domain.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceServiceTest {

    @Mock ServiceRepository serviceRepository;
    @Mock ServiceProviderRepository serviceProviderRepository;
    @Mock BookingRepository bookingRepository;

    ServiceService service;

    @BeforeEach
    void setUp() {
        service = new ServiceService(serviceRepository, serviceProviderRepository, bookingRepository);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_missingProviderId_throwsIllegalArgument() {
        CreateServiceRequest request = new CreateServiceRequest();
        request.setName("Hotel XYZ");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("providerId is required");
    }

    @Test
    void create_providerNotFound_throwsIllegalArgument() {
        UUID providerId = UUID.randomUUID();
        CreateServiceRequest request = new CreateServiceRequest();
        request.setProviderId(providerId);
        request.setName("Hotel XYZ");
        when(serviceProviderRepository.findById(providerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Provider not found");
    }

    @Test
    void create_validRequest_savesAndReturnsResponse() {
        UUID providerId = UUID.randomUUID();
        CreateServiceRequest request = new CreateServiceRequest();
        request.setProviderId(providerId);
        request.setName("Hotel XYZ");
        request.setType(ServiceType.HOTEL);
        request.setBasePrice(BigDecimal.valueOf(100));

        when(serviceProviderRepository.findById(providerId))
                .thenReturn(Optional.of(new ServiceProvider()));

        com.ziyara.backend.domain.entity.Service saved = new com.ziyara.backend.domain.entity.Service();
        saved.setId(UUID.randomUUID());
        saved.setName("Hotel XYZ");
        saved.setProviderId(providerId);
        saved.setType(ServiceType.HOTEL);
        when(serviceRepository.save(any())).thenReturn(saved);

        ServiceResponse result = service.create(request);

        assertThat(result.getName()).isEqualTo("Hotel XYZ");
        verify(serviceRepository).save(any());
    }

    @Test
    void create_nullCurrency_defaultsToUsd() {
        UUID providerId = UUID.randomUUID();
        CreateServiceRequest request = new CreateServiceRequest();
        request.setProviderId(providerId);
        request.setName("Test Service");

        when(serviceProviderRepository.findById(providerId))
                .thenReturn(Optional.of(new ServiceProvider()));

        com.ziyara.backend.domain.entity.Service saved = new com.ziyara.backend.domain.entity.Service();
        saved.setId(UUID.randomUUID());
        saved.setCurrency("USD");
        when(serviceRepository.save(any())).thenReturn(saved);

        service.create(request);

        ArgumentCaptor<com.ziyara.backend.domain.entity.Service> captor =
                ArgumentCaptor.forClass(com.ziyara.backend.domain.entity.Service.class);
        verify(serviceRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrency()).isEqualTo("USD");
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_serviceNotFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(serviceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, new UpdateServiceRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_existingService_updatesFields() {
        UUID id = UUID.randomUUID();
        com.ziyara.backend.domain.entity.Service svc = new com.ziyara.backend.domain.entity.Service();
        svc.setId(id);
        svc.setName("Old Name");
        when(serviceRepository.findById(id)).thenReturn(Optional.of(svc));
        when(serviceRepository.save(any())).thenReturn(svc);

        UpdateServiceRequest request = new UpdateServiceRequest();
        request.setName("New Name");

        service.update(id, request);

        assertThat(svc.getName()).isEqualTo("New Name");
        verify(serviceRepository).save(svc);
    }

    // ── checkAvailability ─────────────────────────────────────────────────────

    @Test
    void checkAvailability_serviceNotFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(serviceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkAvailability(id, null, 0))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void checkAvailability_noDate_availableRoomsExist_returnsAvailable() {
        UUID id = UUID.randomUUID();
        com.ziyara.backend.domain.entity.Service svc = new com.ziyara.backend.domain.entity.Service();
        svc.setId(id);
        svc.setAvailableRooms(5);
        when(serviceRepository.findById(id)).thenReturn(Optional.of(svc));

        ServiceAvailabilityResponse result = service.checkAvailability(id, null, 0);

        assertThat(result.isAvailable()).isTrue();
    }

    @Test
    void checkAvailability_noDate_noAvailableRooms_returnsUnavailable() {
        UUID id = UUID.randomUUID();
        com.ziyara.backend.domain.entity.Service svc = new com.ziyara.backend.domain.entity.Service();
        svc.setId(id);
        svc.setAvailableRooms(0);
        svc.setTotalRooms(10);
        when(serviceRepository.findById(id)).thenReturn(Optional.of(svc));

        ServiceAvailabilityResponse result = service.checkAvailability(id, null, 0);

        assertThat(result.isAvailable()).isFalse();
    }

    @Test
    void checkAvailability_withDate_noOverlappingBookings_returnsAvailable() {
        UUID id = UUID.randomUUID();
        com.ziyara.backend.domain.entity.Service svc = new com.ziyara.backend.domain.entity.Service();
        svc.setId(id);
        svc.setTotalRooms(5);
        when(serviceRepository.findById(id)).thenReturn(Optional.of(svc));
        when(bookingRepository.findOverlappingBookings(any(), any(), any())).thenReturn(List.of());

        ServiceAvailabilityResponse result = service.checkAvailability(id, LocalDate.now(), 2);

        assertThat(result.isAvailable()).isTrue();
    }

    @Test
    void checkAvailability_withDate_allRoomsOccupied_returnsUnavailable() {
        UUID id = UUID.randomUUID();
        com.ziyara.backend.domain.entity.Service svc = new com.ziyara.backend.domain.entity.Service();
        svc.setId(id);
        svc.setTotalRooms(2);
        when(serviceRepository.findById(id)).thenReturn(Optional.of(svc));

        Booking booking = new Booking();
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setRooms(2);
        when(bookingRepository.findOverlappingBookings(any(), any(), any())).thenReturn(List.of(booking));

        ServiceAvailabilityResponse result = service.checkAvailability(id, LocalDate.now(), 2);

        assertThat(result.isAvailable()).isFalse();
        assertThat(result.getMessage()).isNotBlank();
    }
}
