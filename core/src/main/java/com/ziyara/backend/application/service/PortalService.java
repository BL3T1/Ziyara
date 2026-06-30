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
import com.ziyara.backend.application.dto.request.CreateDiscountRequest;
import com.ziyara.backend.application.dto.request.CreateNotificationRequest;
import com.ziyara.backend.application.dto.request.CreatePortalDiscountRequest;
import com.ziyara.backend.application.dto.request.PayoutRequestPayload;
import com.ziyara.backend.application.dto.response.DiscountResponse;
import com.ziyara.backend.application.dto.response.PayoutRequestResponse;
import com.ziyara.backend.application.dto.response.PortalDiscountBalanceResponse;
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
import com.ziyara.backend.modules.discount.api.DiscountCodeApi;
import com.ziyara.backend.modules.notification.api.NotificationServiceApi;
import com.ziyara.backend.modules.provider.api.ProviderProfileEditApi;
import com.ziyara.backend.modules.service.api.HotelRoomApi;
import com.ziyara.backend.modules.service.api.RestaurantMenuApi;
import com.ziyara.backend.modules.service.api.ServiceImageApi;
import com.ziyara.backend.modules.service.api.ServiceServiceApi;
import com.ziyara.backend.modules.webhook.api.WebhookEventPublisher;
import com.ziyara.backend.domain.entity.Booking;
import com.ziyara.backend.domain.entity.ProviderProfileEditRequest;
import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.domain.enums.BookingStatus;
import com.ziyara.backend.domain.enums.NotificationChannel;
import com.ziyara.backend.domain.enums.NotificationType;
import com.ziyara.backend.domain.enums.ServiceImageCategory;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.entity.ProviderDiscountBalance;
import com.ziyara.backend.domain.entity.ServiceEarningData;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.DiscountCodeRepository;
import com.ziyara.backend.domain.repository.PortalEarningsRepository;
import com.ziyara.backend.domain.repository.PortalPayoutRequestRepository;
import com.ziyara.backend.domain.repository.ProviderDiscountBalanceRepository;
import com.ziyara.backend.domain.repository.UserRepository;
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
    private final PaymentRepository paymentRepository;
    private final ServiceServiceApi serviceService;
    private final ServiceImageApi serviceImageService;
    private final RestaurantMenuApi restaurantMenuService;
    private final HotelRoomApi hotelRoomService;
    private final NotificationServiceApi notificationService;
    private final UserRepository userRepository;
    private final DiscountCodeApi discountCodeService;
    private final DiscountCodeRepository discountCodeRepository;
    private final WebhookEventPublisher webhookEventPublisher;
    private final ProviderProfileEditApi profileEditService;
    private final PortalPayoutRequestRepository payoutRequestRepository;
    private final PortalEarningsRepository portalEarningsRepository;
    private final ProviderDiscountBalanceRepository discountBalanceRepository;

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

        // 30-day trend comparison
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime cutLast = now.minusDays(30);
        java.time.LocalDateTime cutPrev = now.minusDays(60);
        long bookingsLast30 = bookings.stream()
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isAfter(cutLast))
                .count();
        long bookingsPrev30 = bookings.stream()
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isAfter(cutPrev) && !b.getCreatedAt().isAfter(cutLast))
                .count();

        List<UUID> last30Ids = bookings.stream()
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isAfter(cutLast))
                .map(Booking::getId)
                .collect(Collectors.toList());
        List<UUID> prev30Ids = bookings.stream()
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isAfter(cutPrev) && !b.getCreatedAt().isAfter(cutLast))
                .map(Booking::getId)
                .collect(Collectors.toList());
        BigDecimal revenueLast30 = last30Ids.isEmpty() ? BigDecimal.ZERO
                : paymentRepository.sumCompletedAmountByBookingIds(last30Ids);
        BigDecimal revenuePrev30 = prev30Ids.isEmpty() ? BigDecimal.ZERO
                : paymentRepository.sumCompletedAmountByBookingIds(prev30Ids);

        return PortalDashboardResponse.builder()
                .serviceCount(serviceCount)
                .totalBookings(totalBookings)
                .activeBookings(activeBookings)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .revenueCurrency(CURRENCY)
                .weeklyRevenue(weeklyRevenue)
                .bookingsLast30Days(bookingsLast30)
                .bookingsPrev30Days(bookingsPrev30)
                .revenueLast30Days(revenueLast30 != null ? revenueLast30 : BigDecimal.ZERO)
                .revenuePrev30Days(revenuePrev30 != null ? revenuePrev30 : BigDecimal.ZERO)
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
        ServiceResponse result = serviceService.create(withProvider);
        try {
            notifyAdminsListingChange(result.getName(), "submitted for review");
        } catch (Exception e) {
            log.warn("Notification dispatch failed after service create: {}", e.getMessage());
        }
        return result;
    }

    @Transactional
    public ServiceResponse updateService(UUID providerId, UUID serviceId, UpdateServiceRequest request) {
        ensureProviderExists(providerId);
        com.ziyara.backend.domain.entity.Service svc = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        if (!providerId.equals(svc.getProviderId())) {
            throw new ResourceNotFoundException("Service not found or access denied");
        }
        ServiceResponse result = serviceService.update(serviceId, request);
        try {
            notifyAdminsListingChange(result.getName(), "updated — pending re-review");
        } catch (Exception e) {
            log.warn("Notification dispatch failed after service update: {}", e.getMessage());
        }
        return result;
    }

    private void notifyAdminsListingChange(String serviceName, String action) {
        userRepository.findByRole(UserRole.SUPER_ADMIN).forEach(admin ->
            notificationService.createNotification(CreateNotificationRequest.builder()
                    .userId(admin.getId())
                    .type(NotificationType.SYSTEM_ALERT)
                    .channel(NotificationChannel.IN_APP)
                    .title("Listing " + action)
                    .message("\"" + serviceName + "\" was " + action + ".")
                    .build())
        );
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

    @Transactional(readOnly = true)
    public List<HotelRoomResponse> getFilteredRooms(UUID providerId, UUID serviceId,
                                                      Integer floor, String category, String status) {
        assertProviderOwnsService(providerId, serviceId);
        com.ziyara.backend.domain.enums.HotelRoomStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            statusEnum = com.ziyara.backend.domain.enums.HotelRoomStatus.valueOf(status.trim().toUpperCase());
        }
        return hotelRoomService.listFiltered(serviceId, floor, category, statusEnum);
    }

    @Transactional(readOnly = true)
    public List<Integer> getRoomFloors(UUID providerId, UUID serviceId) {
        assertProviderOwnsService(providerId, serviceId);
        return hotelRoomService.getDistinctFloors(serviceId);
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

        // Provider commission rate (platform fee %). Default 10% if not set.
        BigDecimal commissionPct = serviceProviderRepository.findById(providerId)
                .map(sp -> sp.getCommissionRate() != null ? sp.getCommissionRate() : BigDecimal.TEN)
                .orElse(BigDecimal.TEN);

        final BigDecimal cpct = commissionPct;
        List<PortalEarningsResponse.ServiceEarningRow> rows = portalEarningsRepository
                .findServiceEarnings(providerId, start, end).stream()
                .map(d -> {
                    BigDecimal fee = d.grossRevenue().multiply(cpct)
                            .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                    return PortalEarningsResponse.ServiceEarningRow.builder()
                            .serviceId(d.serviceId())
                            .serviceName(d.serviceName())
                            .bookingCount(d.bookingCount())
                            .grossRevenue(d.grossRevenue())
                            .platformFee(fee)
                            .providerNet(d.grossRevenue().subtract(fee))
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal grossTotal = rows.stream()
                .map(PortalEarningsResponse.ServiceEarningRow::getGrossRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal feeTotal = grossTotal.multiply(commissionPct)
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal netTotal = grossTotal.subtract(feeTotal);
        int bookingCount = rows.stream().mapToInt(PortalEarningsResponse.ServiceEarningRow::getBookingCount).sum();

        BigDecimal pendingPayouts = payoutRequestRepository.sumPendingAmountByProvider(providerId);
        BigDecimal availableForPayout = netTotal.subtract(pendingPayouts).max(BigDecimal.ZERO);

        return PortalEarningsResponse.builder()
                .start(start)
                .end(end)
                .totalEarnings(grossTotal)   // backward compat
                .currency(CURRENCY)
                .grossRevenue(grossTotal)
                .platformCommissionPct(commissionPct)
                .platformFee(feeTotal)
                .providerNet(netTotal)
                .availableForPayout(availableForPayout)
                .bookingCount(bookingCount)
                .perServiceBreakdown(rows)
                .build();
    }

    public byte[] buildEarningsCsv(PortalEarningsResponse e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Period,\"").append(e.getStart() != null ? e.getStart() : "All time")
          .append(" – ").append(e.getEnd() != null ? e.getEnd() : "All time").append("\"\n");
        sb.append("Platform commission,").append(e.getPlatformCommissionPct()).append("%\n");
        sb.append("Currency,").append(e.getCurrency()).append("\n\n");
        sb.append("Service,Bookings,Gross Revenue,Platform Fee,Your Net\n");
        if (e.getPerServiceBreakdown() != null) {
            for (PortalEarningsResponse.ServiceEarningRow row : e.getPerServiceBreakdown()) {
                sb.append(csvCell(row.getServiceName())).append(",")
                  .append(row.getBookingCount()).append(",")
                  .append(row.getGrossRevenue().toPlainString()).append(",")
                  .append(row.getPlatformFee().toPlainString()).append(",")
                  .append(row.getProviderNet().toPlainString()).append("\n");
            }
        }
        sb.append("TOTAL,").append(e.getBookingCount()).append(",")
          .append(e.getGrossRevenue() != null ? e.getGrossRevenue().toPlainString() : "0").append(",")
          .append(e.getPlatformFee() != null ? e.getPlatformFee().toPlainString() : "0").append(",")
          .append(e.getProviderNet() != null ? e.getProviderNet().toPlainString() : "0").append("\n");
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String csvCell(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<PayoutRequestResponse> listPayoutRequests(UUID providerId, int page, int size) {
        ensureProviderExists(providerId);
        long total = payoutRequestRepository.countByProviderId(providerId);
        List<PayoutRequestResponse> content = payoutRequestRepository.findByProviderId(providerId, size, (long) page * size)
                .stream()
                .map(r -> PayoutRequestResponse.builder()
                        .id(r.getId())
                        .amount(r.getAmount())
                        .currency(r.getCurrency())
                        .notes(r.getNotes())
                        .status(r.getStatus())
                        .requestedAt(r.getRequestedAt())
                        .build())
                .collect(Collectors.toList());
        return new org.springframework.data.domain.PageImpl<>(
                content,
                org.springframework.data.domain.PageRequest.of(page, size),
                total
        );
    }

    @Transactional
    public PayoutRequestResponse createPayoutRequest(UUID providerId, PayoutRequestPayload payload) {
        ensureProviderExists(providerId);
        com.ziyara.backend.domain.entity.PortalPayoutRequest payout =
                new com.ziyara.backend.domain.entity.PortalPayoutRequest();
        payout.setId(UUID.randomUUID());
        payout.setProviderId(providerId);
        payout.setAmount(payload.getAmount());
        payout.setCurrency(CURRENCY);
        payout.setNotes(payload.getNotes());
        payout.setStatus("PENDING");
        payout.setRequestedAt(java.time.Instant.now());
        payoutRequestRepository.save(payout);

        webhookEventPublisher.publishAfterCommit("payout.processed", Map.of(
                "payoutId", payout.getId().toString(),
                "providerId", providerId.toString(),
                "amount", payload.getAmount(),
                "currency", CURRENCY,
                "status", "PENDING"
        ));

        return PayoutRequestResponse.builder()
                .id(payout.getId())
                .amount(payload.getAmount())
                .currency(CURRENCY)
                .status("PENDING")
                .requestedAt(payout.getRequestedAt())
                .build();
    }

    // ── Self-discount engine ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PortalDiscountBalanceResponse getDiscountBalance(UUID providerId) {
        ensureProviderExists(providerId);
        return discountBalanceRepository.findByProviderId(providerId)
                .map(b -> PortalDiscountBalanceResponse.builder()
                        .providerId(providerId)
                        .currency(b.getCurrency())
                        .allocatedAmount(b.getAllocatedAmount())
                        .spentAmount(b.getSpentAmount())
                        .availableAmount(b.getAvailableAmount())
                        .build())
                .orElse(PortalDiscountBalanceResponse.builder()
                        .providerId(providerId).currency("USD")
                        .allocatedAmount(BigDecimal.ZERO).spentAmount(BigDecimal.ZERO).availableAmount(BigDecimal.ZERO)
                        .build());
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<DiscountResponse> listProviderDiscounts(UUID providerId, int page, int size) {
        ensureProviderExists(providerId);
        long total = discountCodeRepository.countByProviderId(providerId);
        List<DiscountResponse> content = discountCodeRepository.findByProviderId(providerId, size, (long) page * size)
                .stream()
                .map(d -> DiscountResponse.builder()
                        .id(d.getId())
                        .code(d.getCode())
                        .description(d.getDescription())
                        .type(d.getType())
                        .value(d.getValue())
                        .minBookingAmount(d.getMinBookingAmount())
                        .maxDiscountAmount(d.getMaxDiscountAmount())
                        .endDate(d.getEndDate())
                        .usageLimit(d.getUsageLimit())
                        .usageCount(d.getUsageCount())
                        .status(d.getStatus())
                        .createdAt(d.getCreatedAt())
                        .sponsor(d.getSponsor())
                        .providerId(providerId)
                        .build())
                .collect(Collectors.toList());
        return new org.springframework.data.domain.PageImpl<>(
                content,
                org.springframework.data.domain.PageRequest.of(page, size),
                total
        );
    }

    @Transactional
    public DiscountResponse createProviderDiscount(UUID providerId, CreatePortalDiscountRequest req) {
        ensureProviderExists(providerId);
        BigDecimal debitAmount = req.getValue();
        // Pessimistic lock: prevents two concurrent requests from both reading sufficient balance
        ProviderDiscountBalance balance = discountBalanceRepository.lockByProviderId(providerId).orElse(null);
        if (balance == null || balance.getAvailableAmount().compareTo(debitAmount) < 0) {
            String available = balance == null ? "0" : balance.getAvailableAmount().toPlainString();
            String currency  = balance == null ? "" : " " + balance.getCurrency();
            throw new com.ziyara.backend.application.exception.BusinessException(
                    "Insufficient discount balance. Available: " + available + currency);
        }
        discountBalanceRepository.debitSpent(providerId, debitAmount);
        CreateDiscountRequest createReq = CreateDiscountRequest.builder()
                .code(req.getCode().trim().toUpperCase())
                .type(req.getType())
                .value(req.getValue())
                .description(req.getDescription())
                .endDate(req.getEndDate())
                .usageLimit(req.getUsageLimit())
                .minBookingAmount(req.getMinBookingAmount())
                .maxDiscountAmount(req.getMaxDiscountAmount())
                .applicableServiceIds(req.getApplicableServiceIds())
                .sponsor("PROVIDER")
                .providerId(providerId)
                .build();
        DiscountResponse created = discountCodeService.create(createReq, null, false);
        discountBalanceRepository.recordDebit(providerId, created.getId(), debitAmount, "Code: " + created.getCode());
        return created;
    }

    @Transactional
    public void deactivateProviderDiscount(UUID providerId, UUID discountId) {
        ensureProviderExists(providerId);
        if (!discountCodeRepository.existsByIdAndProviderId(discountId, providerId)) {
            throw new ResourceNotFoundException("Discount not found or access denied");
        }
        discountCodeService.deactivate(discountId);
    }

    @Transactional
    public PortalDiscountBalanceResponse grantDiscountBalance(UUID providerId, BigDecimal grantAmount, String currency) {
        ensureProviderExists(providerId);
        if (grantAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Grant amount must be positive");
        }
        discountBalanceRepository.upsertAllocated(providerId, grantAmount, currency);
        return getDiscountBalance(providerId);
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public ProviderProfileEditRequest submitProfileEditRequest(UUID providerId, UUID requestedBy,
                                                                 java.util.Map<String, Object> newValues) {
        ensureProviderExists(providerId);
        return profileEditService.submitEditRequest(providerId, requestedBy, newValues);
    }

    @Transactional(readOnly = true)
    public ProviderProfileEditRequest getPendingEditRequest(UUID providerId) {
        ensureProviderExists(providerId);
        return profileEditService.getLatestForProvider(providerId);
    }

    @Transactional
    public void updateProviderLogo(UUID providerId, String logoUrl) {
        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
        provider.setLogoUrl(logoUrl);
        serviceProviderRepository.save(provider);
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
        r.setCheckInTime(request.getCheckInTime());
        r.setCheckOutTime(request.getCheckOutTime());
        r.setLatitude(request.getLatitude());
        r.setLongitude(request.getLongitude());
        r.setPolicies(request.getPolicies());
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
                .paymentMethod(b.getPaymentMethod())
                .paymentStatus(b.getPaymentStatus() != null ? b.getPaymentStatus().name() : null)
                .build();
    }
}
