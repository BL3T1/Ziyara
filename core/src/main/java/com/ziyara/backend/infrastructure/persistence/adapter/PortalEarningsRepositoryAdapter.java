package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.ServiceEarningData;
import com.ziyara.backend.domain.repository.PortalEarningsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PortalEarningsRepositoryAdapter implements PortalEarningsRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<ServiceEarningData> findServiceEarnings(UUID providerId, LocalDate start, LocalDate end) {
        List<Object> params = new ArrayList<>();
        StringBuilder dateFilter = new StringBuilder();
        if (start != null) {
            dateFilter.append(" AND b.created_at >= CAST(? AS TIMESTAMPTZ)");
            params.add(start.toString());
        }
        if (end != null) {
            dateFilter.append(" AND b.created_at < (CAST(? AS DATE) + INTERVAL '1 day')::TIMESTAMPTZ");
            params.add(end.toString());
        }
        params.add(providerId);

        String sql =
            "SELECT hs.id::text AS service_id, hs.name AS service_name, " +
            "COUNT(DISTINCT b.id) AS booking_count, " +
            "COALESCE(SUM(CASE WHEN p.status = 'COMPLETED' THEN p.amount ELSE 0 END), 0) AS gross_revenue " +
            "FROM hotel_services hs " +
            "LEFT JOIN bkg_bookings b ON b.service_id = hs.id" + dateFilter + " " +
            "LEFT JOIN pay_payments p ON p.booking_id = b.id " +
            "WHERE hs.provider_id = ? AND hs.deleted_at IS NULL " +
            "GROUP BY hs.id, hs.name ORDER BY gross_revenue DESC LIMIT 20";

        return jdbcTemplate.query(sql, params.toArray(), (rs, rn) -> {
            BigDecimal gross = rs.getBigDecimal("gross_revenue");
            return new ServiceEarningData(
                    UUID.fromString(rs.getString("service_id")),
                    rs.getString("service_name"),
                    rs.getInt("booking_count"),
                    gross != null ? gross : BigDecimal.ZERO
            );
        });
    }
}
