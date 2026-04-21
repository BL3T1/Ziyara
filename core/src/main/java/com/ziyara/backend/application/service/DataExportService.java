package com.ziyara.backend.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziyara.backend.application.event.DataExportRequestedEvent;
import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.infrastructure.persistence.entity.BookingJpaEntity;
import com.ziyara.backend.infrastructure.persistence.entity.DataExportRequestJpaEntity;
import com.ziyara.backend.infrastructure.persistence.entity.NotificationJpaEntity;
import com.ziyara.backend.infrastructure.persistence.entity.UserConsentJpaEntity;
import com.ziyara.backend.infrastructure.persistence.repository.BookingJpaRepository;
import com.ziyara.backend.infrastructure.persistence.repository.DataExportRequestJpaRepository;
import com.ziyara.backend.infrastructure.persistence.repository.NotificationJpaRepository;
import com.ziyara.backend.infrastructure.persistence.repository.UserConsentJpaRepository;
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

    private final DataExportRequestJpaRepository exportRepository;
    private final UserRepository userRepository;
    private final BookingJpaRepository bookingJpaRepository;
    private final UserConsentJpaRepository userConsentJpaRepository;
    private final NotificationJpaRepository notificationJpaRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public List<DataExportRequestJpaEntity> list(UUID userId) {
        return exportRepository.findByUserIdOrderByRequestedAtDesc(userId);
    }

    public DataExportRequestJpaEntity getForUser(UUID exportId, UUID userId) {
        DataExportRequestJpaEntity row = exportRepository.findById(exportId)
                .orElseThrow(() -> new IllegalArgumentException("Export not found"));
        if (!row.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Export not found");
        }
        return row;
    }

    public byte[] downloadPayloadForUser(UUID exportId, UUID userId) {
        DataExportRequestJpaEntity row = getForUser(exportId, userId);
        if (!"COMPLETED".equals(row.getStatus()) || row.getPayloadJson() == null || row.getPayloadJson().isBlank()) {
            throw new IllegalStateException("Export is not ready for download");
        }
        return row.getPayloadJson().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public DataExportRequestJpaEntity requestExport(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found");
        }
        DataExportRequestJpaEntity row = DataExportRequestJpaEntity.builder()
                .userId(userId)
                .status("PENDING")
                .format("JSON")
                .requestedAt(LocalDateTime.now())
                .build();
        row = exportRepository.save(row);
        eventPublisher.publishEvent(new DataExportRequestedEvent(row.getId()));
        return row;
    }

    @Transactional
    public void completeExport(UUID exportRequestId) {
        DataExportRequestJpaEntity row = exportRepository.findById(exportRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Export not found"));
        if (!"PENDING".equals(row.getStatus())) {
            return;
        }
        try {
            UUID userId = row.getUserId();
            User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));

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
            for (UserConsentJpaEntity c : userConsentJpaRepository.findByUserIdOrderByGrantedAtDesc(userId)) {
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

            var bookingPage = bookingJpaRepository.findByCustomerId(
                    userId,
                    PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "createdAt")));
            List<Map<String, Object>> bookings = new ArrayList<>();
            for (BookingJpaEntity b : bookingPage.getContent()) {
                bookings.add(bookingSummary(b));
            }
            payload.put("bookings", bookings);
            payload.put("bookingsTruncated", bookingPage.getTotalElements() > bookings.size());

            List<Map<String, Object>> notifications = new ArrayList<>();
            for (NotificationJpaEntity n : notificationJpaRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId)) {
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

    private static Map<String, Object> bookingSummary(BookingJpaEntity b) {
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
