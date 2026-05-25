package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.BookingResponse;
import com.ziyara.backend.application.dto.request.CreateMenuItemRequest;
import com.ziyara.backend.application.dto.request.CreateMenuSectionRequest;
import com.ziyara.backend.application.dto.request.CreateHotelRoomRequest;
import com.ziyara.backend.application.dto.request.CreateServiceImageRequest;
import com.ziyara.backend.application.dto.request.CreateServiceRequest;
import com.ziyara.backend.application.dto.request.UpdateMenuItemRequest;
import com.ziyara.backend.application.dto.request.UpdateMenuSectionRequest;
import com.ziyara.backend.application.dto.request.UpdateHotelRoomRequest;
import com.ziyara.backend.application.dto.request.UpdateServiceImageRequest;
import com.ziyara.backend.application.dto.request.UpdateServiceRequest;
import com.ziyara.backend.application.dto.request.PayoutRequestPayload;
import com.ziyara.backend.application.dto.response.PayoutRequestResponse;
import com.ziyara.backend.application.dto.response.PortalDashboardResponse;
import com.ziyara.backend.application.dto.response.PortalEarningsResponse;
import com.ziyara.backend.application.dto.response.RestaurantMenuItemResponse;
import com.ziyara.backend.application.dto.response.RestaurantMenuResponse;
import com.ziyara.backend.application.dto.response.RestaurantMenuSectionResponse;
import com.ziyara.backend.application.dto.response.HotelRoomImageResponse;
import com.ziyara.backend.application.dto.response.HotelRoomResponse;
import com.ziyara.backend.application.dto.response.ServiceImageResponse;
import com.ziyara.backend.application.dto.response.ServiceResponse;
import com.ziyara.backend.application.query.ServiceQueryHandler;
import com.ziyara.backend.modules.service.api.ServiceServiceApi;
import com.ziyara.backend.domain.entity.Booking;
import com.ziyara.backend.domain.enums.BookingStatus;
import com.ziyara.backend.domain.enums.ServiceImageCategory;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.PaymentRepository;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.domain.repository.ServiceRepository;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Provider-scoped portal: dashboard, services, bookings, earnings (BACKEND_CRUD_REPORT Â§4).
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
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final PaymentRepository paymentRepository;
    private final ServiceServiceApi serviceService;
    private final ServiceImageService serviceImageService;
    private final RestaurantMenuService restaurantMenuService;
    private final HotelRoomService hotelRoomService;

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

        List<PortalDashboardResponse.WeeklyRevenueItem> weeklyRevenue =
                buildWeeklyRevenue(bookingIds);

        return PortalDashboardResponse.builder()
                .serviceCount(serviceCount)
                .totalBookings(totalBookings)
                .activeBookings(activeBookings)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .revenueCurrency(CURRENCY)
                .weeklyRevenue(weeklyRevenue)
                .build();
    }

    private List<PortalDashboardResponse.WeeklyRevenueItem> buildWeeklyRevenue(List<UUID> bookingIds) {
        if (bookingIds.isEmpty()) return buildEmptyWeeks();
        LocalDateTime since = LocalDateTime.now().minusWeeks(8).with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay();
        var payments = paymentRepository.findCompletedByBookingIdsSince(bookingIds, since);

        // Group by ISO week start (Monday)
        Map<LocalDate, BigDecimal> byWeek = new TreeMap<>();
        for (var p : payments) {
            LocalDate weekStart = p.getCreatedAt().toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            byWeek.merge(weekStart, p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO, BigDecimal::add);
        }

        // Ensure all 8 weeks are present (fill gaps with zero)
        LocalDate cursor = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(7);
        List<PortalDashboardResponse.WeeklyRevenueItem> result = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            result.add(PortalDashboardResponse.WeeklyRevenueItem.builder()
                    .week(cursor.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .amount(byWeek.getOrDefault(cursor, BigDecimal.ZERO))
                    .build());
            cursor = cursor.plusWeeks(1);
        }
        return result;
    }

    private List<PortalDashboardResponse.WeeklyRevenueItem> buildEmptyWeeks() {
        LocalDate cursor = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(7);
        List<PortalDashboardResponse.WeeklyRevenueItem> result = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            result.add(PortalDashboardResponse.WeeklyRevenueItem.builder()
                    .week(cursor.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .amount(BigDecimal.ZERO)
                    .build());
            cursor = cursor.plusWeeks(1);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Page<ServiceResponse> getServices(UUID providerId, int page, int size) {
        ensureProviderExists(providerId);
        return serviceQueryHandler.findPage(page, size, providerId, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public ServiceResponse getService(UUID providerId, UUID serviceId) {
        assertProviderOwnsService(providerId, serviceId);
        return serviceQueryHandler.findById(serviceId)
                .orElseThrow(() -> new com.ziyara.backend.application.exception.ResourceNotFoundException("Service not found"));
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

    @Transactional
    public RestaurantMenuItemResponse uploadMenuItemImage(
            UUID providerId,
            UUID serviceId,
            UUID itemId,
            byte[] fileBytes,
            String contentType,
            String originalFilename) {
        assertProviderOwnsService(providerId, serviceId);
        return restaurantMenuService.uploadItemImage(serviceId, itemId, fileBytes, contentType, originalFilename);
    }

    @Transactional(readOnly = true)
    public List<HotelRoomResponse> getHotelRooms(UUID providerId, UUID serviceId) {
        assertProviderOwnsService(providerId, serviceId);
        return hotelRoomService.listByService(serviceId);
    }

    @Transactional
    public HotelRoomResponse createHotelRoom(UUID providerId, UUID serviceId, CreateHotelRoomRequest request) {
        assertProviderOwnsService(providerId, serviceId);
        return hotelRoomService.create(serviceId, request);
    }

    @Transactional
    public HotelRoomResponse updateHotelRoom(
            UUID providerId, UUID serviceId, UUID roomId, UpdateHotelRoomRequest request) {
        assertProviderOwnsService(providerId, serviceId);
        return hotelRoomService.update(serviceId, roomId, request);
    }

    @Transactional
    public void deleteHotelRoom(UUID providerId, UUID serviceId, UUID roomId) {
        assertProviderOwnsService(providerId, serviceId);
        hotelRoomService.delete(serviceId, roomId);
    }

    @Transactional
    public HotelRoomImageResponse uploadRoomImage(
            UUID providerId,
            UUID serviceId,
            UUID roomId,
            byte[] fileBytes,
            String contentType,
            String originalFilename,
            String altText,
            Boolean primary) {
        assertProviderOwnsService(providerId, serviceId);
        return hotelRoomService.uploadRoomImage(
                serviceId, roomId, fileBytes, contentType, originalFilename, altText, primary);
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

    @Transactional
    public PayoutRequestResponse createPayoutRequest(UUID providerId, PayoutRequestPayload payload) {
        ensureProviderExists(providerId);
        UUID id = UUID.randomUUID();
        java.time.Instant now = java.time.Instant.now();
        jdbcTemplate.update(
                "INSERT INTO portal_payout_requests (id, provider_id, amount, currency, notes, status, requested_at) " +
                "VALUES (?, ?, ?, ?, ?, 'PENDING', ?)",
                id, providerId, payload.getAmount(), CURRENCY,
                payload.getNotes(), java.sql.Timestamp.from(now));
        return PayoutRequestResponse.builder()
                .id(id)
                .amount(payload.getAmount())
                .currency(CURRENCY)
                .status("PENDING")
                .requestedAt(now)
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
