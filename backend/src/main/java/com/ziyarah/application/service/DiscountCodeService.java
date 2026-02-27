package com.ziyarah.application.service;

import com.ziyarah.domain.entity.DiscountCode;
import com.ziyarah.domain.enums.DiscountStatus;
import com.ziyarah.domain.repository.DiscountCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Service: DiscountCodeService
 * Handles promotion and discount code logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountCodeService {
    
    private final DiscountCodeRepository discountCodeRepository;
    
    @Transactional(readOnly = true)
    public Optional<DiscountCode> validateCode(String code, BigDecimal bookingAmount) {
        log.info("Validating discount code: {}", code);
        
        return discountCodeRepository.findByCode(code)
                .filter(dc -> dc.getStatus() == DiscountStatus.ACTIVE)
                .filter(dc -> dc.getEndDate().isAfter(java.time.LocalDateTime.now()))
                .filter(dc -> dc.getMinBookingAmount() == null || bookingAmount.compareTo(dc.getMinBookingAmount()) >= 0);
    }
    
    @Transactional
    public void recordUsage(String code) {
        log.info("Recording usage for discount code: {}", code);
        
        discountCodeRepository.findByCode(code).ifPresent(dc -> {
            dc.incrementUsage();
            discountCodeRepository.save(dc);
        });
    }
}
