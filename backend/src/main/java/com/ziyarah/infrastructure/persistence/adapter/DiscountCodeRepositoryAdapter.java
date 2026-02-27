package com.ziyarah.infrastructure.persistence.adapter;

import com.ziyarah.domain.entity.DiscountCode;
import com.ziyarah.domain.enums.DiscountStatus;
import com.ziyarah.domain.repository.DiscountCodeRepository;
import com.ziyarah.infrastructure.persistence.entity.DiscountCodeJpaEntity;
import com.ziyarah.infrastructure.persistence.mapper.DiscountCodeMapper;
import com.ziyarah.infrastructure.persistence.repository.DiscountCodeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository Adapter: DiscountCodeRepositoryAdapter
 */
@Component
@RequiredArgsConstructor
public class DiscountCodeRepositoryAdapter implements DiscountCodeRepository {
    
    private final DiscountCodeJpaRepository discountCodeJpaRepository;
    private final DiscountCodeMapper discountCodeMapper;
    
    @Override
    public DiscountCode save(DiscountCode discountCode) {
        DiscountCodeJpaEntity entity = discountCodeMapper.toJpaEntity(discountCode);
        DiscountCodeJpaEntity savedEntity = discountCodeJpaRepository.save(entity);
        return discountCodeMapper.toDomainEntity(savedEntity);
    }
    
    @Override
    public Optional<DiscountCode> findById(UUID id) {
        return discountCodeJpaRepository.findById(id)
                .map(discountCodeMapper::toDomainEntity);
    }
    
    @Override
    public Optional<DiscountCode> findByCode(String code) {
        return discountCodeJpaRepository.findByCode(code)
                .map(discountCodeMapper::toDomainEntity);
    }
    
    @Override
    public List<DiscountCode> findByStatus(DiscountStatus status) {
        return discountCodeJpaRepository.findByStatus(status).stream()
                .map(discountCodeMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<DiscountCode> findAll() {
        return discountCodeJpaRepository.findAll().stream()
                .map(discountCodeMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(UUID id) {
        discountCodeJpaRepository.deleteById(id);
    }
    
    @Override
    public boolean existsByCode(String code) {
        return discountCodeJpaRepository.existsByCode(code);
    }
}
