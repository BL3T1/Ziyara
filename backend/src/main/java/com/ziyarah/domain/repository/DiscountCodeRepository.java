package com.ziyarah.domain.repository;

import com.ziyarah.domain.entity.DiscountCode;
import com.ziyarah.domain.enums.DiscountStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository Port: DiscountCodeRepository
 */
public interface DiscountCodeRepository {
    DiscountCode save(DiscountCode discountCode);
    Optional<DiscountCode> findById(UUID id);
    Optional<DiscountCode> findByCode(String code);
    List<DiscountCode> findByStatus(DiscountStatus status);
    List<DiscountCode> findAll();
    void deleteById(UUID id);
    boolean existsByCode(String code);
}
