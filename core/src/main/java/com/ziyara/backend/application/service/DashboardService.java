package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.ActivityFeedItemResponse;
import com.ziyara.backend.application.dto.response.DashboardKpiResponse;
import com.ziyara.backend.domain.entity.AuditLog;
import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.enums.BookingStatus;
import com.ziyara.backend.domain.enums.TicketStatus;
import com.ziyara.backend.domain.repository.AuditLogRepository;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.ComplaintRepository;
import com.ziyara.backend.domain.repository.InternalTicketRepository;
import com.ziyara.backend.domain.repository.PaymentRepository;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.infrastructure.config.DashboardExecutorConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Dashboard KPIs and activity feed (DASHBOARD_DESIGN_REPORT â€“ Command Center).
 */
@Service
@Slf4j
public class DashboardService {

    private static final String REVENUE_CURRENCY = "USD";

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final InternalTicketRepository internalTicketRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final Executor dashboardExecutor;

    @Autowired(required = false)
    private ComplaintRepository complaintRepository;

    public DashboardService(PaymentRepository paymentRepository,
                            BookingRepository bookingRepository,
                            ServiceProviderRepository serviceProviderRepository,
                            InternalTicketRepository internalTicketRepository,
                            AuditLogRepository auditLogRepository,
                            UserRepository userRepository,
                            @Qualifier(DashboardExecutorConfig.DASHBOARD_EXECUTOR) Executor dashboardExecutor) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.serviceProviderRepository = serviceProviderRepository;
        this.internalTicketRepository = internalTicketRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.dashboardExecutor = dashboardExecutor;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public DashboardKpiResponse getKpis(LocalDate start, LocalDate end) {
        try {
            LocalDateTime from = start != null ? start.atStartOfDay() : LocalDate.now().minusMonths(1).atStartOfDay();
            LocalDateTime to = end != null ? end.atTime(LocalTime.MAX) : LocalDateTime.now();

            CompletableFuture<BigDecimal> revenueFut = CompletableFuture.supplyAsync(() -> {
                try {
                    BigDecimal r = paymentRepository.sumCompletedAmountBetween(from, to);
                    return r != null ? r : BigDecimal.ZERO;
                } catch (Exception e) {
                    log.debug("sumCompletedAmountBetween failed: {}", e.getMessage());
                    return BigDecimal.ZERO;
                }
            }, dashboardExecutor);

            CompletableFuture<long[]> bookingsFut = CompletableFuture.supplyAsync(() -> {
                try {
                    long total = bookingRepository.count();
                    long active = bookingRepository.countByStatusIn(
                            Arrays.asList(BookingStatus.CONFIRMED, BookingStatus.ACTIVE));
                    return new long[]{total, active};
                } catch (Exception e) {
                    log.debug("booking counts failed: {}", e.getMessage());
                    return new long[]{0L, 0L};
                }
            }, dashboardExecutor);

            CompletableFuture<Long> providersFut = CompletableFuture.supplyAsync(() -> {
                try {
                    return serviceProviderRepository.count();
                } catch (Exception e) {
                    log.debug("serviceProvider count failed: {}", e.getMessage());
                    return 0L;
                }
            }, dashboardExecutor);

            CompletableFuture<Long> ticketsFut = CompletableFuture.supplyAsync(() -> {
                try {
                    List<TicketStatus> openStatuses = Arrays.asList(
                            TicketStatus.SUBMITTED, TicketStatus.ACKNOWLEDGED, TicketStatus.ASSIGNED,
                            TicketStatus.IN_PROGRESS, TicketStatus.PENDING_INFO, TicketStatus.TESTING, TicketStatus.REOPENED);
                    return internalTicketRepository.countByStatusIn(openStatuses);
                } catch (Exception e) {
                    log.debug("countByStatusIn failed: {}", e.getMessage());
                    return 0L;
                }
            }, dashboardExecutor);

            CompletableFuture<Long> complaintsFut = CompletableFuture.supplyAsync(() -> {
                if (complaintRepository == null) {
                    return 0L;
                }
                try {
                    return complaintRepository.countOpenComplaints();
                } catch (Exception e) {
                    log.debug("countOpenComplaints failed: {}", e.getMessage());
                    return 0L;
                }
            }, dashboardExecutor);

            CompletableFuture.allOf(revenueFut, bookingsFut, providersFut, ticketsFut, complaintsFut).join();

            BigDecimal totalRevenue = revenueFut.join();
            long[] bookings = bookingsFut.join();
            long totalBookings = bookings[0];
            long activeBookings = bookings[1];
            long totalProviders = providersFut.join();
            long openTickets = ticketsFut.join();
            long pendingComplaints = complaintsFut.join();

            return DashboardKpiResponse.builder()
                    .totalRevenue(totalRevenue)
                    .revenueCurrency(REVENUE_CURRENCY)
                    .activeBookings(activeBookings)
                    .totalBookings(totalBookings)
                    .totalProviders(totalProviders)
                    .pendingComplaints(pendingComplaints)
                    .openTickets(openTickets)
                    .build();
        } catch (Exception e) {
            log.warn("getKpis failed: {}", e.getMessage());
            return DashboardKpiResponse.builder()
                    .totalRevenue(BigDecimal.ZERO)
                    .revenueCurrency(REVENUE_CURRENCY)
                    .activeBookings(0)
                    .totalBookings(0)
                    .totalProviders(0)
                    .pendingComplaints(0)
                    .openTickets(0)
                    .build();
        }
    }

    @Transactional(readOnly = true)
    public List<ActivityFeedItemResponse> getActivityFeed(int limit) {
        try {
            List<AuditLog> logs = auditLogRepository.findRecent(PageRequest.of(0, Math.min(limit, 50))).getContent();
            java.util.Set<UUID> userIds = logs.stream()
                    .map(AuditLog::getUserId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<UUID, String> emailByUserId = new HashMap<>();
            if (!userIds.isEmpty()) {
                for (User u : userRepository.findAllById(userIds)) {
                    if (u.getId() != null && u.getEmail() != null) {
                        emailByUserId.put(u.getId(), u.getEmail());
                    }
                }
            }
            return logs.stream()
                    .map(log -> toActivityItem(log, emailByUserId))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.debug("getActivityFeed failed: {}", e.getMessage());
            return List.of();
        }
    }

    private ActivityFeedItemResponse toActivityItem(AuditLog log, Map<UUID, String> emailByUserId) {
        String userDisplay = "system";
        if (log.getUserId() != null) {
            userDisplay = emailByUserId.getOrDefault(log.getUserId(), "system");
        }
        String changeSummary = buildChangeSummary(log.getOldValue(), log.getNewValue());
        return ActivityFeedItemResponse.builder()
                .id(log.getId() != null ? log.getId().toString() : null)
                .action(log.getAction())
                .entityType(log.getEntityName())
                .entityId(log.getEntityId())
                .userDisplay(userDisplay)
                .changeSummary(changeSummary)
                .timestamp(log.getCreatedAt())
                .build();
    }

    private String buildChangeSummary(String oldVal, String newVal) {
        if (oldVal == null && newVal == null) return "";
        if (oldVal == null) return "â†’ " + newVal;
        if (newVal == null) return oldVal + " â†’ ";
        return oldVal + " â†’ " + newVal;
    }
}
