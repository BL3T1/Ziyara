package com.ziyarah.infrastructure.persistence.repository;

import com.ziyarah.domain.enums.TaxiStatus;
import com.ziyarah.infrastructure.persistence.entity.TaxiBookingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository: TaxiBookingJpaRepository
 */
@Repository
public interface TaxiBookingJpaRepository extends JpaRepository<TaxiBookingJpaEntity, UUID> {
    Optional<TaxiBookingJpaEntity> findByBookingId(UUID bookingId);
    List<TaxiBookingJpaEntity> findByStatus(TaxiStatus status);
    List<TaxiBookingJpaEntity> findByDriverId(UUID driverId);
}
