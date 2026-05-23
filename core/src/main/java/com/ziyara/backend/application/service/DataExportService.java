package com.ziyara.backend.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziyara.backend.application.dto.response.DataExportRequestResponse;
import com.ziyara.backend.application.event.DataExportRequestedEvent;
import com.ziyara.backend.domain.entity.Booking;
import com.ziyara.backend.domain.entity.DataExportRequest;
import com.ziyara.backend.domain.entity.Notification;
import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.entity.UserConsent;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.DataExportRequestRepository;
import com.ziyara.backend.domain.repository.NotificationRepository;
import com.ziyara.backend.domain.repository.UserConsentRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GDPR-style portability: enqueue {@code PENDING} exports, materialize JSON asynchronously into {@code payload_json}.
 */
@Service
@RequiredArgsConstructor
public class DataExportService {

    private static final String MANIFEST_VERSION = "ziyara.export.v1";

    private final DataExportRequestRepository exportRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final UserConsentRepository userConsentRepository;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public List<DataExportRequestResponse> list(UUID userId) {
        return exportRepository.findByUserIdOrderedDesc(userId).stream()
                .map(this::toResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    public DataExportRequestResponse getForUser(UUID exportId, UUID userId) {
        return toResponse(getEntityForUser(exportId, userId));
    }

    public byte[] downloadPayloadForUser(UUID exportId, UUID userId) {
        DataExportRequest row = getEntityForUser(exportId, userId);
        if (!"COMPLETED".equals(row.getStatus()) || row.getPayloadJson() == null || row.getPayloadJson().isBlank()) {
            throw new IllegalStateException("Export is not ready for download");
        }
        return row.getPayloadJson().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public DataExportRequestResponse requestExport(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found");
        }
        DataExportRequest row = new DataExportRequest();
        row.setUserId(userId);
        row.setStatus("PENDING");
        row.setFormat("JSON");
        row.setRequestedAt(LocalDateTime.now());
        row = exportRepository.save(row);
        eventPublisher.publishEvent(new DataExportRequestedEvent(row.getId()));
        return toResponse(row);
    }

    private DataExportRequest getEntityForUser(UUID exportId, UUID userId) {
        DataExportRequest row = exportRepository.findById(exportId)
                .orElseThrow(() -> new IllegalArgumentException("Export not found"));
        if (!row.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Export not found");
        }
        return row;
    }

    private DataExportRequestResponse toResponse(DataExportRequest e) {
        return DataExportRequestResponse.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .status(e.getStatus())
                .format(e.getFormat())
                .requestedAt(e.getRequestedAt())
                .completedAt(e.getCompletedAt())
                .expiresAt(e.getExpiresAt())
                .recordCount(e.getRecordCount())
                .failureReason(e.getFailureReason())
                .build();
    }

    @Transactional
    public void completeExport(UUID exportRequestId) {
        DataExportRequest row = exportRepository.findById(exportRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Export not found"));
        if (!"PENDING".equals(row.getStatus())) {
            return;
        }
        try {
            UUID userId = row.getUserId();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("manifestVersion", MANIFEST_VERSION);
            payload.put("userId", user.getId().toString());
            payload.put("email", user.getEmail());
            payload.put("phone", user.getPhone());
            payload.put("role", user.getRole() != null ? user.getRole().name() : null);
            payload.put("status", user.getStatus() != null ? user.getStatus().name() : null);
            payload.put("gdprConsentGiven", user.isGdprConsentGiven());
            payload.put("gdprConsentDate", user.getGdprConsentDate() != null ? user.getGdprConsentDate().toString() : null);
            payload.put("marketingOptIn", user.isMarketingOptIn());
            payload.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
            payload.put("exportedAt", LocalDateTime.now().toString());

            List<Map<String, Object>> consents = new ArrayList<>();
            for (UserConsent c : userConsentRepository.findByUserIdOrderedDesc(userId)) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("consentType", c.getConsentType());
                m.put("purpose", c.getPurpose());
                m.put("granted", c.getGranted());
                m.put("grantedAt", c.getGrantedAt() != null ? c.getGrantedAt().toString() : null);
                m.put("withdrawnAt", c.getWithdrawnAt() != null ? c.getWithdrawnAt().toString() : null);
                m.put("version", c.getVersion());
                consents.add(m);
            }
            payload.put("consents", consents);

            var bookingPage = bookingRepository.findByCustomerId(
                    userId,
                    PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "createdAt")));
            List<Map<String, Object>> bookings = new ArrayList<>();
            for (Booking b : bookingPage.getContent()) {
                bookings.add(bookingSummary(b));
            }
            payload.put("bookings", bookings);
            payload.put("bookingsTruncated", bookingPage.getTotalElements() > bookings.size());

            List<Map<String, Object>> notifications = new ArrayList<>();
            for (Notification n : notificationRepository
                    .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 50))
                    .getContent()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", n.getId().toString());
                m.put("type", n.getType() != null ? n.getType().name() : null);
                m.put("channel", n.getChannel() != null ? n.getChannel().name() : null);
                m.put("status", n.getStatus() != null ? n.getStatus().name() : null);
                m.put("title", n.getTitle());
                m.put("sentAt", n.getSentAt() != null ? n.getSentAt().toString() : null);
                m.put("readAt", n.getReadAt() != null ? n.getReadAt().toString() : null);
                notifications.add(m);
            }
            payload.put("notifications", notifications);

            String json = objectMapper.writeValueAsString(payload);
            row.setPayloadJson(json);
            row.setRecordCount(1 + consents.size() + bookings.size() + notifications.size());
            row.setStatus("COMPLETED");
            row.setCompletedAt(LocalDateTime.now());
            row.setExpiresAt(LocalDateTime.now().plusDays(7));
            row.setExportPath("inline:payload_json");
            exportRepository.save(row);
        } catch (Exception e) {
            row.setStatus("FAILED");
            row.setFailureReason(e.getMessage());
            exportRepository.save(row);
        }
    }

    private static Map<String, Object> bookingSummary(Booking b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId().toString());
        m.put("bookingReference", b.getBookingReference());
        m.put("status", b.getStatus() != null ? b.getStatus().name() : null);
        m.put("checkInDate", b.getCheckInDate() != null ? b.getCheckInDate().toString() : null);
        m.put("checkOutDate", b.getCheckOutDate() != null ? b.getCheckOutDate().toString() : null);
        m.put("guests", b.getGuests());
        m.put("totalAmount", b.getTotalAmount());
        m.put("currency", b.getCurrency());
        m.put("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().toString() : null);
        return m;
    }
}
