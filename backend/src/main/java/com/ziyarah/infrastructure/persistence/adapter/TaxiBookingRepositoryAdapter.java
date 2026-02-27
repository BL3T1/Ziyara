package com.ziyarah.infrastructure.persistence.adapter;

import com.ziyarah.domain.entity.TaxiBooking;
import com.ziyarah.domain.enums.TaxiStatus;
import com.ziyarah.domain.repository.TaxiBookingRepository;
import com.ziyarah.infrastructure.persistence.entity.TaxiBookingJpaEntity;
import com.ziyarah.infrastructure.persistence.mapper.TaxiBookingMapper;
import com.ziyarah.infrastructure.persistence.repository.TaxiBookingJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository Adapter: TaxiBookingRepositoryAdapter
 */
@Component
@RequiredArgsConstructor
public class TaxiBookingRepositoryAdapter implements TaxiBookingRepository {
    
    private final TaxiBookingJpaRepository taxiBookingJpaRepository;
    private final TaxiBookingMapper taxiBookingMapper;
    
    @Override
    public TaxiBooking save(TaxiBooking taxiBooking) {
        TaxiBookingJpaEntity entity = taxiBookingMapper.toJpaEntity(taxiBooking);
        TaxiBookingJpaEntity savedEntity = taxiBookingJpaRepository.save(entity);
        return taxiBookingMapper.toDomainEntity(savedEntity);
    }
    
    @Override
    public Optional<TaxiBooking> findById(UUID id) {
        return taxiBookingJpaRepository.findById(id)
                .map(taxiBookingMapper::toDomainEntity);
    }
    
    @Override
    public Optional<TaxiBooking> findByBookingId(UUID bookingId) {
        return taxiBookingJpaRepository.findByBookingId(bookingId)
                .map(taxiBookingMapper::toDomainEntity);
    }
    
    @Override
    public List<TaxiBooking> findByStatus(TaxiStatus status) {
        return taxiBookingJpaRepository.findByStatus(status).stream()
                .map(taxiBookingMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
}
