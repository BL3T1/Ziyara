package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.DeliveryLocation;
import com.ziyara.backend.domain.repository.DeliveryLocationRepository;
import com.ziyara.backend.infrastructure.persistence.repository.DeliveryLocationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeliveryLocationRepositoryAdapter implements DeliveryLocationRepository {

    private final DeliveryLocationJpaRepository jpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<DeliveryLocation> findLatestByBookingId(UUID bookingId) {
        return jpaRepository.findTopByBookingIdOrderByRecordedAtDesc(bookingId)
                .map(e -> new DeliveryLocation(
                        e.getId(),
                        e.getBookingId(),
                        e.getLatitude(),
                        e.getLongitude(),
                        e.getStatus(),
                        e.getRecordedAt()));
    }
}
