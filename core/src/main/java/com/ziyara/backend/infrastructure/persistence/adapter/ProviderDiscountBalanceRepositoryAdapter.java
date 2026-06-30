package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.ProviderDiscountBalance;
import com.ziyara.backend.domain.repository.ProviderDiscountBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProviderDiscountBalanceRepositoryAdapter implements ProviderDiscountBalanceRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<ProviderDiscountBalance> findByProviderId(UUID providerId) {
        List<ProviderDiscountBalance> result = jdbcTemplate.query(
                "SELECT provider_id, currency, allocated_amount, spent_amount " +
                "FROM provider_discount_balance WHERE provider_id = ?",
                (rs, rn) -> mapRow(rs), providerId);
        return result.stream().findFirst();
    }

    @Override
    public Optional<ProviderDiscountBalance> lockByProviderId(UUID providerId) {
        List<ProviderDiscountBalance> result = jdbcTemplate.query(
                "SELECT provider_id, currency, allocated_amount, spent_amount " +
                "FROM provider_discount_balance WHERE provider_id = ? FOR UPDATE",
                (rs, rn) -> mapRow(rs), providerId);
        return result.stream().findFirst();
    }

    @Override
    public void debitSpent(UUID providerId, BigDecimal amount) {
        jdbcTemplate.update(
                "UPDATE provider_discount_balance SET spent_amount = spent_amount + ?, " +
                "updated_at = CURRENT_TIMESTAMP WHERE provider_id = ?",
                amount, providerId);
    }

    @Override
    public void upsertAllocated(UUID providerId, BigDecimal grantAmount, String currency) {
        jdbcTemplate.update(
                "INSERT INTO provider_discount_balance (provider_id, currency, allocated_amount) VALUES (?, ?, ?) " +
                "ON CONFLICT (provider_id) DO UPDATE SET " +
                "allocated_amount = provider_discount_balance.allocated_amount + ?, " +
                "currency = EXCLUDED.currency, updated_at = CURRENT_TIMESTAMP",
                providerId, currency != null ? currency : "USD", grantAmount, grantAmount);
    }

    @Override
    public void recordDebit(UUID providerId, UUID discountCodeId, BigDecimal amount, String description) {
        jdbcTemplate.update(
                "INSERT INTO provider_discount_debits (provider_id, discount_code_id, amount, description) VALUES (?, ?, ?, ?)",
                providerId, discountCodeId, amount, description);
    }

    private ProviderDiscountBalance mapRow(ResultSet rs) throws SQLException {
        ProviderDiscountBalance b = new ProviderDiscountBalance();
        b.setProviderId(UUID.fromString(rs.getString("provider_id")));
        b.setCurrency(rs.getString("currency"));
        b.setAllocatedAmount(rs.getBigDecimal("allocated_amount"));
        b.setSpentAmount(rs.getBigDecimal("spent_amount"));
        return b;
    }
}
