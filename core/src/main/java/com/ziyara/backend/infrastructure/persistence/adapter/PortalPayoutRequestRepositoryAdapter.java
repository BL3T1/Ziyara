package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.PortalPayoutRequest;
import com.ziyara.backend.domain.repository.PortalPayoutRequestRepository;
import com.ziyara.backend.infrastructure.persistence.entity.PortalPayoutRequestJpaEntity;
import com.ziyara.backend.infrastructure.persistence.repository.PortalPayoutRequestJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PortalPayoutRequestRepositoryAdapter implements PortalPayoutRequestRepository {

    private final PortalPayoutRequestJpaRepository jpaRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public PortalPayoutRequest save(PortalPayoutRequest request) {
        PortalPayoutRequestJpaEntity entity = toJpa(request);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<PortalPayoutRequest> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<PortalPayoutRequest> findFiltered(String status, UUID providerId,
                                                   String start, String end, String q,
                                                   int limit, long offset) {
        List<Object> params = new ArrayList<>();
        String where = buildFilterWhere(status, providerId, start, end, q, params);
        params.add(limit);
        params.add(offset);
        String sql = "SELECT r.id, r.provider_id, r.amount, r.currency, r.notes, r.status," +
                " r.requested_at, r.processed_at, r.processed_by, r.rejection_reason, r.transaction_id," +
                " r.scheduled_at, r.is_manual" +
                " FROM portal_payout_requests r" + where +
                " ORDER BY r.requested_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, params.toArray(), (rs, rn) -> mapRow(rs));
    }

    @Override
    public long countFiltered(String status, UUID providerId, String start, String end, String q) {
        List<Object> params = new ArrayList<>();
        String where = buildFilterWhere(status, providerId, start, end, q, params);
        Long result = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM portal_payout_requests r" + where, Long.class, params.toArray());
        return result != null ? result : 0L;
    }

    @Override
    public List<PortalPayoutRequest> findByProviderId(UUID providerId, int limit, long offset) {
        return jpaRepository.findByProviderIdPaged(providerId, limit, offset)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public long countByProviderId(UUID providerId) {
        return jpaRepository.countByProviderId(providerId);
    }

    @Override
    public List<PortalPayoutRequest> findForExport(String status, String start, String end) {
        List<Object> params = new ArrayList<>();
        String where = "WHERE 1=1";
        where += buildDateFilter(start, end, "r.requested_at", params);
        if (status != null && !status.isBlank()) {
            where += " AND r.status = ?";
            params.add(status.toUpperCase());
        }
        String sql = "SELECT r.id, r.provider_id, r.amount, r.currency, r.notes, r.status," +
                " r.requested_at, r.processed_at, r.processed_by, r.rejection_reason, r.transaction_id," +
                " r.scheduled_at, r.is_manual" +
                " FROM portal_payout_requests r " + where +
                " ORDER BY r.requested_at DESC";
        return jdbcTemplate.query(sql, params.toArray(), (rs, rn) -> mapRow(rs));
    }

    @Override
    public BigDecimal sumAmountByStatus(String status) {
        BigDecimal result = jpaRepository.sumAmountByStatus(status);
        return result != null ? result : BigDecimal.ZERO;
    }

    @Override
    public long countByStatus(String status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    public BigDecimal sumCompletedAmountInPeriod(String start, String end) {
        List<Object> params = new ArrayList<>();
        String dateFilter = buildDateFilter(start, end, "r.requested_at", params);
        String sql = "SELECT COALESCE(SUM(r.amount), 0) FROM portal_payout_requests r" +
                " WHERE r.status = 'COMPLETED'" + dateFilter;
        BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class, params.toArray());
        return result != null ? result : BigDecimal.ZERO;
    }

    @Override
    public long countByStatuses(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) return 0L;
        return jpaRepository.countByStatuses(statuses);
    }

    @Override
    public BigDecimal sumPendingAmountByProvider(UUID providerId) {
        BigDecimal result = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM portal_payout_requests WHERE provider_id = ? AND status = 'PENDING'",
                BigDecimal.class, providerId);
        return result != null ? result : BigDecimal.ZERO;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildFilterWhere(String status, UUID providerId,
                                    String start, String end, String q,
                                    List<Object> params) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (status != null && !status.isBlank()) {
            where.append(" AND r.status = ?");
            params.add(status.toUpperCase());
        }
        if (providerId != null) {
            where.append(" AND r.provider_id = ?::uuid");
            params.add(providerId.toString());
        }
        where.append(buildDateFilter(start, end, "r.requested_at", params));
        if (q != null && !q.isBlank()) {
            where.append(" AND CAST(r.id AS TEXT) LIKE ?");
            params.add("%" + q.toLowerCase() + "%");
        }
        return where.toString();
    }

    private String buildDateFilter(String start, String end, String col, List<Object> params) {
        StringBuilder sb = new StringBuilder();
        if (start != null && !start.isBlank()) {
            sb.append(" AND ").append(col).append(" >= ?::date");
            params.add(start);
        }
        if (end != null && !end.isBlank()) {
            sb.append(" AND ").append(col).append(" <= ?::date + interval '1 day'");
            params.add(end);
        }
        return sb.toString();
    }

    private PortalPayoutRequest mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        PortalPayoutRequest r = new PortalPayoutRequest();
        r.setId(UUID.fromString(rs.getString("id")));
        r.setProviderId(UUID.fromString(rs.getString("provider_id")));
        r.setAmount(rs.getBigDecimal("amount"));
        r.setCurrency(rs.getString("currency"));
        r.setNotes(rs.getString("notes"));
        r.setStatus(rs.getString("status"));
        Timestamp reqAt = rs.getTimestamp("requested_at");
        if (reqAt != null) r.setRequestedAt(reqAt.toInstant());
        Timestamp procAt = rs.getTimestamp("processed_at");
        if (procAt != null) r.setProcessedAt(procAt.toInstant());
        String processedBy = rs.getString("processed_by");
        if (processedBy != null) r.setProcessedBy(UUID.fromString(processedBy));
        r.setRejectionReason(rs.getString("rejection_reason"));
        r.setTransactionId(rs.getString("transaction_id"));
        r.setScheduledAt(rs.getString("scheduled_at"));
        r.setManual(rs.getBoolean("is_manual"));
        return r;
    }

    private PortalPayoutRequest toDomain(PortalPayoutRequestJpaEntity e) {
        PortalPayoutRequest r = new PortalPayoutRequest();
        r.setId(e.getId());
        r.setProviderId(e.getProviderId());
        r.setAmount(e.getAmount());
        r.setCurrency(e.getCurrency());
        r.setNotes(e.getNotes());
        r.setStatus(e.getStatus());
        r.setRequestedAt(e.getRequestedAt());
        r.setProcessedAt(e.getProcessedAt());
        r.setProcessedBy(e.getProcessedBy());
        r.setRejectionReason(e.getRejectionReason());
        r.setTransactionId(e.getTransactionId());
        r.setScheduledAt(e.getScheduledAt());
        r.setManual(e.isManual());
        return r;
    }

    private PortalPayoutRequestJpaEntity toJpa(PortalPayoutRequest r) {
        return PortalPayoutRequestJpaEntity.builder()
                .id(r.getId() != null ? r.getId() : UUID.randomUUID())
                .providerId(r.getProviderId())
                .amount(r.getAmount())
                .currency(r.getCurrency())
                .notes(r.getNotes())
                .status(r.getStatus())
                .requestedAt(r.getRequestedAt() != null ? r.getRequestedAt() : Instant.now())
                .processedAt(r.getProcessedAt())
                .processedBy(r.getProcessedBy())
                .rejectionReason(r.getRejectionReason())
                .transactionId(r.getTransactionId())
                .scheduledAt(r.getScheduledAt())
                .manual(r.isManual())
                .build();
    }
}
