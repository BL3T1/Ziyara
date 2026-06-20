package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.DeliveryLocationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryLocationJpaRepository extends JpaRepository<DeliveryLocationJpaEntity, UUID> {
    Optional<DeliveryLocationJpaEntity> findTopByBookingIdOrderByRecordedAtDesc(UUID bookingId);
}
