package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.AdminPayoutActionRequest;
import com.ziyara.backend.application.dto.request.BulkPayoutActionRequest;
import com.ziyara.backend.application.dto.request.CreateManualPayoutRequest;
import com.ziyara.backend.application.dto.response.AdminPayoutResponse;
import com.ziyara.backend.application.dto.response.AdminPayoutSummaryResponse;
import com.ziyara.backend.application.annotation.Audited;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPayoutService {

    private static final String CURRENCY = "USD";

    private final JdbcTemplate jdbcTemplate;

    // -------------------------------------------------------------------------
    // Query
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public AdminPayoutSummaryResponse getSummary(String start, String end) {
        String dateFilter = buildDateFilter(start, end, "r.requested_at");

        BigDecimal totalPayable = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM portal_payout_requests WHERE status = 'PENDING'",
                BigDecimal.class);

        Long pendingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM portal_payout_requests WHERE status = 'PENDING'",
                Long.class);

        Long processingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM portal_payout_requests WHERE status = 'PROCESSING'",
                Long.class);

        String completedQuery = "SELECT COALESCE(SUM(r.amount), 0) FROM portal_payout_requests r WHERE r.status = 'COMPLETED'" + dateFilter;
        BigDecimal totalCompleted = jdbcTemplate.queryForObject(completedQuery, BigDecimal.class);

        Long failedOnHold = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM portal_payout_requests WHERE status IN ('FAILED', 'ON_HOLD')",
                Long.class);

        return AdminPayoutSummaryResponse.builder()
                .totalPayable(totalPayable != null ? totalPayable : BigDecimal.ZERO)
                .pendingCount(pendingCount != null ? pendingCount : 0L)
                .processingCount(processingCount != null ? processingCount : 0L)
                .totalCompletedInPeriod(totalCompleted != null ? totalCompleted : BigDecimal.ZERO)
                .failedOnHoldCount(failedOnHold != null ? failedOnHold : 0L)
                .currency(CURRENCY)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<AdminPayoutResponse> listPayouts(int page, int size, String status, String providerId,
                                                  String start, String end, String q) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");

        if (status != null && !status.isBlank()) {
            where.append(" AND r.status = ?");
            params.add(status.toUpperCase());
        }
        if (providerId != null && !providerId.isBlank()) {
            where.append(" AND r.provider_id = ?::uuid");
            params.add(providerId);
        }
        if (start != null && !start.isBlank()) {
            where.append(" AND r.requested_at >= ?::timestamptz");
            params.add(start);
        }
        if (end != null && !end.isBlank()) {
            where.append(" AND r.requested_at <= ?::timestamptz");
            params.add(end);
        }
        if (q != null && !q.isBlank()) {
            where.append(" AND (LOWER(sp.company_name) LIKE LOWER(?) OR CAST(r.id AS TEXT) LIKE LOWER(?))");
            String likeQ = "%" + q.toLowerCase() + "%";
            params.add(likeQ);
            params.add(likeQ);
        }

        String baseQuery = "FROM portal_payout_requests r" +
                " LEFT JOIN hotel_service_providers sp ON sp.id = r.provider_id";

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*)" + baseQuery + where, Long.class, params.toArray());

        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(size);
        pageParams.add((long) page * size);

        String selectQuery = "SELECT r.id, r.provider_id, r.amount, r.currency, r.notes, r.status," +
                " r.requested_at, r.processed_at, r.processed_by, r.rejection_reason, r.transaction_id," +
                " r.scheduled_at, r.is_manual," +
                " sp.company_name AS provider_name, sp.contact_email AS provider_email" +
                " " + baseQuery + where +
                " ORDER BY r.requested_at DESC LIMIT ? OFFSET ?";

        List<AdminPayoutResponse> content = jdbcTemplate.query(selectQuery, PAYOUT_ROW_MAPPER, pageParams.toArray());

        return new PageImpl<>(content, PageRequest.of(page, size), total != null ? total : 0L);
    }

    @Transactional(readOnly = true)
    public AdminPayoutResponse getById(UUID id) {
        List<AdminPayoutResponse> results = jdbcTemplate.query(
                "SELECT r.id, r.provider_id, r.amount, r.currency, r.notes, r.status," +
                " r.requested_at, r.processed_at, r.processed_by, r.rejection_reason, r.transaction_id," +
                " r.scheduled_at, r.is_manual," +
                " sp.company_name AS provider_name, sp.contact_email AS provider_email" +
                " FROM portal_payout_requests r" +
                " LEFT JOIN hotel_service_providers sp ON sp.id = r.provider_id" +
                " WHERE r.id = ?",
                PAYOUT_ROW_MAPPER, id);

        if (results.isEmpty()) {
            throw new ResourceNotFoundException("Payout request not found: " + id);
        }
        return results.get(0);
    }

    // -------------------------------------------------------------------------
    // Status transitions
    // -------------------------------------------------------------------------

    @Audited(action = "PAYOUT_APPROVE", entityType = "Payout", entityIdArgIndex = 0)
    @Transactional
    public AdminPayoutResponse approve(UUID id, AdminPayoutActionRequest req) {
        requireStatus(id, "PENDING", "SCHEDULED");
        UUID user = currentUser();
        jdbcTemplate.update(
                "UPDATE portal_payout_requests SET status='PROCESSING', processed_at=?, processed_by=?, notes=? WHERE id=?",
                Timestamp.from(Instant.now()), user, req != null ? req.getNotes() : null, id);
        return getById(id);
    }

    @Audited(action = "PAYOUT_HOLD", entityType = "Payout", entityIdArgIndex = 0)
    @Transactional
    public AdminPayoutResponse hold(UUID id) {
        requireStatus(id, "PENDING");
        jdbcTemplate.update("UPDATE portal_payout_requests SET status='ON_HOLD' WHERE id=?", id);
        return getById(id);
    }

    @Audited(action = "PAYOUT_RELEASE_HOLD", entityType = "Payout", entityIdArgIndex = 0)
    @Transactional
    public AdminPayoutResponse releaseHold(UUID id) {
        requireStatus(id, "ON_HOLD");
        jdbcTemplate.update("UPDATE portal_payout_requests SET status='PENDING' WHERE id=?", id);
        return getById(id);
    }

    @Audited(action = "PAYOUT_CANCEL", entityType = "Payout", entityIdArgIndex = 0)
    @Transactional
    public AdminPayoutResponse cancel(UUID id) {
        requireStatus(id, "PENDING", "ON_HOLD", "SCHEDULED");
        jdbcTemplate.update("UPDATE portal_payout_requests SET status='CANCELLED' WHERE id=?", id);
        return getById(id);
    }

    @Audited(action = "PAYOUT_RETRY", entityType = "Payout", entityIdArgIndex = 0)
    @Transactional
    public AdminPayoutResponse retry(UUID id) {
        requireStatus(id, "FAILED", "REJECTED");
        jdbcTemplate.update("UPDATE portal_payout_requests SET status='PENDING' WHERE id=?", id);
        return getById(id);
    }

    @Audited(action = "PAYOUT_MARK_PAID", entityType = "Payout", entityIdArgIndex = 0)
    @Transactional
    public AdminPayoutResponse markPaid(UUID id, AdminPayoutActionRequest req) {
        UUID user = currentUser();
        jdbcTemplate.update(
                "UPDATE portal_payout_requests SET status='COMPLETED', processed_at=?, processed_by=?," +
                " transaction_id=?, notes=? WHERE id=?",
                Timestamp.from(Instant.now()), user,
                req != null ? req.getTransactionId() : null,
                req != null ? req.getNotes() : null, id);
        return getById(id);
    }

    @Audited(action = "PAYOUT_SCHEDULE", entityType = "Payout", entityIdArgIndex = 0)
    @Transactional
    public AdminPayoutResponse schedule(UUID id, AdminPayoutActionRequest req) {
        requireStatus(id, "PENDING");
        jdbcTemplate.update(
                "UPDATE portal_payout_requests SET status='SCHEDULED', scheduled_at=?::timestamptz WHERE id=?",
                req != null ? req.getScheduledAt() : null, id);
        return getById(id);
    }

    @Transactional
    public AdminPayoutResponse updateNotes(UUID id, String notes) {
        jdbcTemplate.update("UPDATE portal_payout_requests SET notes=? WHERE id=?", notes, id);
        return getById(id);
    }

    // -------------------------------------------------------------------------
    // Bulk operations
    // -------------------------------------------------------------------------

    @Audited(action = "PAYOUT_BULK_APPROVE", entityType = "Payout")
    @Transactional
    public Map<String, Object> bulkApprove(BulkPayoutActionRequest req) {
        UUID user = currentUser();
        Instant now = Instant.now();
        int count = 0;
        List<String> failed = new ArrayList<>();
        for (UUID id : req.getIds()) {
            try {
                requireStatus(id, "PENDING", "SCHEDULED");
                jdbcTemplate.update(
                        "UPDATE portal_payout_requests SET status='PROCESSING', processed_at=?, processed_by=? WHERE id=?",
                        Timestamp.from(now), user, id);
                count++;
            } catch (Exception e) {
                failed.add(id.toString());
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("processed", count);
        result.put("failed", failed);
        return result;
    }

    @Audited(action = "PAYOUT_BULK_HOLD", entityType = "Payout")
    @Transactional
    public Map<String, Object> bulkHold(BulkPayoutActionRequest req) {
        int count = 0;
        for (UUID id : req.getIds()) {
            int rows = jdbcTemplate.update(
                    "UPDATE portal_payout_requests SET status='ON_HOLD' WHERE id=? AND status='PENDING'", id);
            count += rows;
        }
        return Map.of("processed", count);
    }

    @Audited(action = "PAYOUT_BULK_RELEASE_HOLD", entityType = "Payout")
    @Transactional
    public Map<String, Object> bulkReleaseHold(BulkPayoutActionRequest req) {
        int count = 0;
        for (UUID id : req.getIds()) {
            int rows = jdbcTemplate.update(
                    "UPDATE portal_payout_requests SET status='PENDING' WHERE id=? AND status='ON_HOLD'", id);
            count += rows;
        }
        return Map.of("processed", count);
    }

    // -------------------------------------------------------------------------
    // Manual payout creation
    // -------------------------------------------------------------------------

    @Audited(action = "PAYOUT_CREATE_MANUAL", entityType = "Payout")
    @Transactional
    public AdminPayoutResponse createManual(CreateManualPayoutRequest req) {
        UUID id = UUID.randomUUID();
        String status = req.isExecuteImmediately() ? "PROCESSING" : "PENDING";
        UUID user = currentUser();
        jdbcTemplate.update(
                "INSERT INTO portal_payout_requests (id, provider_id, amount, currency, notes, status, requested_at, is_manual, processed_by)" +
                " VALUES (?, ?, ?, 'USD', ?, ?, NOW(), TRUE, ?)",
                id, req.getProviderId(), req.getAmount(), req.getMemo(), status, user);
        return getById(id);
    }

    // -------------------------------------------------------------------------
    // CSV Export
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public byte[] exportCsv(String status, String start, String end) {
        String dateFilter = buildDateFilter(start, end, "r.requested_at");
        String where = "WHERE 1=1" + dateFilter;
        if (status != null && !status.isBlank()) {
            where += " AND r.status = '" + status.replace("'", "''") + "'";
        }
        String sql = "SELECT r.id, sp.company_name AS provider_name, sp.contact_email AS provider_email, " +
                     "r.amount, r.currency, r.status, r.requested_at, r.processed_at, " +
                     "r.transaction_id, r.notes, r.is_manual " +
                     "FROM portal_payout_requests r " +
                     "LEFT JOIN hotel_service_providers sp ON sp.id = r.provider_id " +
                     where + " ORDER BY r.requested_at DESC";
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Provider,Email,Amount,Currency,Status,Requested At,Processed At,Transaction ID,Notes,Manual\n");
        jdbcTemplate.query(sql, rs -> {
            csv.append('"').append(rs.getString("id")).append('"').append(',');
            csv.append('"').append(safe(rs.getString("provider_name"))).append('"').append(',');
            csv.append('"').append(safe(rs.getString("provider_email"))).append('"').append(',');
            csv.append(rs.getBigDecimal("amount")).append(',');
            csv.append(safe(rs.getString("currency"))).append(',');
            csv.append(safe(rs.getString("status"))).append(',');
            Timestamp ra = rs.getTimestamp("requested_at");
            csv.append(ra != null ? ra.toInstant().toString() : "").append(',');
            Timestamp pa = rs.getTimestamp("processed_at");
            csv.append(pa != null ? pa.toInstant().toString() : "").append(',');
            csv.append('"').append(safe(rs.getString("transaction_id"))).append('"').append(',');
            csv.append('"').append(safe(rs.getString("notes"))).append('"').append(',');
            csv.append(rs.getBoolean("is_manual")).append('\n');
        });
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("\"", "\"\"");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void requireStatus(UUID id, String... allowedStatuses) {
        String current = jdbcTemplate.queryForObject(
                "SELECT status FROM portal_payout_requests WHERE id=?", String.class, id);
        if (current == null) throw new ResourceNotFoundException("Payout request not found: " + id);
        for (String s : allowedStatuses) {
            if (s.equals(current)) return;
        }
        throw new IllegalStateException(
                "Cannot perform action on payout in status " + current +
                ". Allowed: " + Arrays.toString(allowedStatuses));
    }

    private UUID currentUser() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) {
                return UUID.fromString(auth.getName());
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String buildDateFilter(String start, String end, String col) {
        if ((start == null || start.isBlank()) && (end == null || end.isBlank())) return "";
        StringBuilder sb = new StringBuilder();
        if (start != null && !start.isBlank()) sb.append(" AND ").append(col).append(" >= '").append(start).append("'::date");
        if (end != null && !end.isBlank()) sb.append(" AND ").append(col).append(" <= '").append(end).append("'::date + interval '1 day'");
        return sb.toString();
    }

    private static final RowMapper<AdminPayoutResponse> PAYOUT_ROW_MAPPER = (rs, rowNum) -> {
        Timestamp reqAt = rs.getTimestamp("requested_at");
        Timestamp procAt = rs.getTimestamp("processed_at");
        Timestamp schedAt = rs.getTimestamp("scheduled_at");

        return AdminPayoutResponse.builder()
                .id(UUID.fromString(rs.getString("id")))
                .providerId(UUID.fromString(rs.getString("provider_id")))
                .providerName(rs.getString("provider_name"))
                .providerEmail(rs.getString("provider_email"))
                .amount(rs.getBigDecimal("amount"))
                .currency(rs.getString("currency") != null ? rs.getString("currency") : "USD")
                .notes(rs.getString("notes"))
                .status(rs.getString("status"))
                .requestedAt(reqAt != null ? reqAt.toInstant() : null)
                .processedAt(procAt != null ? procAt.toInstant() : null)
                .processedBy(rs.getString("processed_by"))
                .rejectionReason(rs.getString("rejection_reason"))
                .transactionId(rs.getString("transaction_id"))
                .scheduledAt(schedAt != null ? schedAt.toInstant() : null)
                .manual(rs.getBoolean("is_manual"))
                .statusHistory(List.of())
                .build();
    };
}
