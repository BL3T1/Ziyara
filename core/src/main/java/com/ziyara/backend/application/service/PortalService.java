package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.BookingResponse;
import com.ziyara.backend.application.dto.request.CreateMenuItemRequest;
import com.ziyara.backend.application.dto.request.CreateMenuSectionRequest;
import com.ziyara.backend.application.dto.request.CreateServiceImageRequest;
import com.ziyara.backend.application.dto.request.CreateServiceRequest;
import com.ziyara.backend.application.dto.request.UpdateMenuItemRequest;
import com.ziyara.backend.application.dto.request.UpdateMenuSectionRequest;
import com.ziyara.backend.application.dto.request.UpdateServiceImageRequest;
import com.ziyara.backend.application.dto.request.UpdateServiceRequest;
import com.ziyara.backend.application.dto.response.PortalDashboardResponse;
import com.ziyara.backend.application.dto.response.PortalEarningsResponse;
import com.ziyara.backend.application.dto.response.RestaurantMenuItemResponse;
import com.ziyara.backend.application.dto.response.RestaurantMenuResponse;
import com.ziyara.backend.application.dto.response.RestaurantMenuSectionResponse;
import com.ziyara.backend.application.dto.response.ServiceImageResponse;
import com.ziyara.backend.application.dto.response.ServiceResponse;
import com.ziyara.backend.application.query.ServiceQueryHandler;
import com.ziyara.backend.domain.entity.Booking;
import com.ziyara.backend.domain.enums.BookingStatus;
import com.ziyara.backend.domain.enums.ServiceImageCategory;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.PaymentRepository;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.domain.repository.ServiceRepository;
import com.ziyara.backend.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Provider-scoped portal: dashboard, services, bookings, earnings (BACKEND_CRUD_REPORT §4).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortalService {

    private static final String CURRENCY = "USD";

    private final ServiceProviderRepository serviceProviderRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceQueryHandler serviceQueryHandler;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final ServiceService serviceService;
    private final ServiceImageService serviceImageService;
    private final RestaurantMenuService restaurantMenuService;

    @Transactional(readOnly = true)
    public PortalDashboardResponse getDashboard(UUID providerId) {
        ensureProviderExists(providerId);
        List<UUID> serviceIds = serviceRepository.findByProviderId(providerId).stream()
                .map(com.ziyara.backend.domain.entity.Service::getId)
                .collect(Collectors.toList());
        long serviceCount = serviceIds.size();
        List<Booking> bookings = serviceIds.isEmpty() ? List.of() : bookingRepository.findByServiceIdIn(serviceIds);
        long totalBookings = bookings.size();
        long activeBookings = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.ACTIVE)
                .count();
        List<UUID> bookingIds = bookings.stream().map(Booking::getId).collect(Collectors.toList());
        BigDecimal totalRevenue = bookingIds.isEmpty() ? BigDecimal.ZERO
                : paymentRepository.sumCompletedAmountByBookingIds(bookingIds);

        return PortalDashboardResponse.builder()
                .serviceCount(serviceCount)
                .totalBookings(totalBookings)
                .activeBookings(activeBookings)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .revenueCurrency(CURRENCY)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ServiceResponse> getServices(UUID providerId, int page, int size) {
        ensureProviderExists(providerId);
        return serviceQueryHandler.findPage(page, size, providerId, null, null, null, null);
    }

    @Transactional
    public ServiceResponse createService(UUID providerId, CreateServiceRequest request) {
        ensureProviderExists(providerId);
        CreateServiceRequest withProvider = copyWithProviderId(request, providerId);
        return serviceService.create(withProvider);
    }

    @Transactional
    public ServiceResponse updateService(UUID providerId, UUID serviceId, UpdateServiceRequest request) {
        ensureProviderExists(providerId);
        com.ziyara.backend.domain.entity.Service svc = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        if (!providerId.equals(svc.getProviderId())) {
            throw new ResourceNotFoundException("Service not found or access denied");
        }
        return serviceService.update(serviceId, request);
    }

    @Transactional
    public void deleteService(UUID providerId, UUID serviceId) {
        ensureProviderExists(providerId);
        com.ziyara.backend.domain.entity.Service svc = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        if (!providerId.equals(svc.getProviderId())) {
            throw new ResourceNotFoundException("Service not found or access denied");
        }
        serviceService.deleteById(serviceId);
    }

    @Transactional(readOnly = true)
    public List<ServiceImageResponse> getServiceImages(UUID providerId, UUID serviceId) {
        assertProviderOwnsService(providerId, serviceId);
        return serviceImageService.list(serviceId);
    }

    @Transactional
    public ServiceImageResponse createServiceImage(UUID providerId, UUID serviceId, CreateServiceImageRequest request) {
        assertProviderOwnsService(providerId, serviceId);
        return serviceImageService.create(serviceId, request);
    }

    @Transactional
    public ServiceImageResponse updateServiceImage(
            UUID providerId, UUID serviceId, UUID imageId, UpdateServiceImageRequest request) {
        assertProviderOwnsService(providerId, serviceId);
        return serviceImageService.update(serviceId, imageId, request);
    }

    @Transactional
    public void deleteServiceImage(UUID providerId, UUID serviceId, UUID imageId) {
        assertProviderOwnsService(providerId, serviceId);
        serviceImageService.delete(serviceId, imageId);
    }

    @Transactional
    public ServiceImageResponse uploadServiceImage(
            UUID providerId,
            UUID serviceId,
            byte[] fileBytes,
            String contentType,
            String originalFilename,
            String altText,
            ServiceImageCategory category,
            String contextKey,
            Boolean primary) {
        assertProviderOwnsService(providerId, serviceId);
        return serviceImageService.uploadAndCreateImage(
                serviceId, fileBytes, contentType, originalFilename, altText, category, contextKey, primary);
    }

    @Transactional(readOnly = true)
    public RestaurantMenuResponse getRestaurantMenu(UUID providerId, UUID serviceId) {
        assertProviderOwnsService(providerId, serviceId);
        return restaurantMenuService.getMenu(serviceId);
    }

    @Transactional
    public RestaurantMenuSectionResponse createMenuSection(
            UUID providerId, UUID serviceId, CreateMenuSectionRequest request) {
        assertProviderOwnsService(providerId, serviceId);
        return restaurantMenuService.createSection(serviceId, request);
    }

    @Transactional
    public RestaurantMenuSectionResponse updateMenuSection(
            UUID providerId, UUID serviceId, UUID sectionId, UpdateMenuSectionRequest request) {
        assertProviderOwnsService(providerId, serviceId);
        return restaurantMenuService.updateSection(serviceId, sectionId, request);
    }

    @Transactional
    public void deleteMenuSection(UUID providerId, UUID serviceId, UUID sectionId) {
        assertProviderOwnsService(providerId, serviceId);
        restaurantMenuService.deleteSection(serviceId, sectionId);
    }

    @Transactional
    public RestaurantMenuItemResponse createMenuItem(
            UUID providerId, UUID serviceId, UUID sectionId, CreateMenuItemRequest request) {
        assertProviderOwnsService(providerId, serviceId);
        return restaurantMenuService.createItem(serviceId, sectionId, request);
    }

    @Transactional
    public RestaurantMenuItemResponse updateMenuItem(
            UUID providerId, UUID serviceId, UUID itemId, UpdateMenuItemRequest request) {
        assertProviderOwnsService(providerId, serviceId);
        return restaurantMenuService.updateItem(serviceId, itemId, request);
    }

    @Transactional
    public void deleteMenuItem(UUID providerId, UUID serviceId, UUID itemId) {
        assertProviderOwnsService(providerId, serviceId);
        restaurantMenuService.deleteItem(serviceId, itemId);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookings(UUID providerId) {
        ensureProviderExists(providerId);
        List<UUID> serviceIds = serviceRepository.findByProviderId(providerId).stream()
                .map(com.ziyara.backend.domain.entity.Service::getId)
                .collect(Collectors.toList());
        if (serviceIds.isEmpty()) return List.of();
        return bookingRepository.findByServiceIdIn(serviceIds).stream()
                .map(PortalService::toBookingResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PortalEarningsResponse getEarnings(UUID providerId, LocalDate start, LocalDate end) {
        ensureProviderExists(providerId);
        List<UUID> serviceIds = serviceRepository.findByProviderId(providerId).stream()
                .map(com.ziyara.backend.domain.entity.Service::getId)
                .collect(Collectors.toList());
        if (serviceIds.isEmpty()) {
            return PortalEarningsResponse.builder()
                    .start(start).end(end).totalEarnings(BigDecimal.ZERO).currency(CURRENCY).build();
        }
        List<Booking> bookings = bookingRepository.findByServiceIdIn(serviceIds);
        List<UUID> bookingIds = bookings.stream().map(Booking::getId).collect(Collectors.toList());
        if (bookingIds.isEmpty()) {
            return PortalEarningsResponse.builder()
                    .start(start).end(end).totalEarnings(BigDecimal.ZERO).currency(CURRENCY).build();
        }
        BigDecimal total = paymentRepository.sumCompletedAmountByBookingIds(bookingIds);
        if (total == null) total = BigDecimal.ZERO;
        return PortalEarningsResponse.builder()
                .start(start)
                .end(end)
                .totalEarnings(total)
                .currency(CURRENCY)
                .build();
    }

    private void ensureProviderExists(UUID providerId) {
        if (serviceProviderRepository.findById(providerId).isEmpty()) {
            throw new ResourceNotFoundException("Provider not found");
        }
    }

    private void assertProviderOwnsService(UUID providerId, UUID serviceId) {
        ensureProviderExists(providerId);
        com.ziyara.backend.domain.entity.Service svc = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        if (!providerId.equals(svc.getProviderId())) {
            throw new ResourceNotFoundException("Service not found or access denied");
        }
    }

    private static CreateServiceRequest copyWithProviderId(CreateServiceRequest request, UUID providerId) {
        CreateServiceRequest r = new CreateServiceRequest();
        r.setProviderId(providerId);
        r.setType(request.getType());
        r.setName(request.getName());
        r.setDescription(request.getDescription());
        r.setCity(request.getCity());
        r.setCountry(request.getCountry());
        r.setAddress(request.getAddress());
        r.setBasePrice(request.getBasePrice());
        r.setCurrency(request.getCurrency());
        r.setMaxGuests(request.getMaxGuests());
        r.setTotalRooms(request.getTotalRooms());
        r.setAvailableRooms(request.getAvailableRooms());
        r.setStarRating(request.getStarRating());
        r.setAttributes(request.getAttributes());
        r.setAmenities(request.getAmenities());
        return r;
    }

    private static BookingResponse toBookingResponse(Booking b) {
        return BookingResponse.builder()
                .id(b.getId())
                .bookingReference(b.getBookingReference())
                .customerId(b.getCustomerId())
                .serviceId(b.getServiceId())
                .checkInDate(b.getCheckInDate())
                .checkOutDate(b.getCheckOutDate())
                .guests(b.getGuests())
                .rooms(b.getRooms())
                .baseAmount(b.getBaseAmount())
                .discountAmount(b.getDiscountAmount())
                .taxAmount(b.getTaxAmount())
                .commissionAmount(b.getCommissionAmount())
                .totalAmount(b.getTotalAmount())
                .currency(b.getCurrency())
                .status(b.getStatus())
                .specialRequests(b.getSpecialRequests())
                .idDocumentVerified(b.isIdDocumentVerified())
                .confirmedAt(b.getConfirmedAt())
                .cancelledAt(b.getCancelledAt())
                .cancellationReason(b.getCancellationReason())
                .createdAt(b.getCreatedAt())
                .canBeCancelled(b.canBeCancelled())
                .canBeModified(b.canBeModified())
                .build();
    }
}
