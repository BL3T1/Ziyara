package com.ziyarah.infrastructure.persistence.repository;

import com.ziyarah.domain.enums.BookingStatus;
import com.ziyarah.infrastructure.persistence.entity.BookingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository: BookingJpaRepository
 * Infrastructure layer implementation for Booking data access
 */
@Repository
public interface BookingJpaRepository extends JpaRepository<BookingJpaEntity, UUID> {
    
    Optional<BookingJpaEntity> findByBookingReference(String bookingReference);
    
    List<BookingJpaEntity> findByCustomerId(UUID customerId);
    
    List<BookingJpaEntity> findByServiceId(UUID serviceId);
    
    List<BookingJpaEntity> findByStatus(BookingStatus status);
    
    List<BookingJpaEntity> findByCustomerIdAndStatus(UUID customerId, BookingStatus status);
    
    List<BookingJpaEntity> findByCheckInDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<BookingJpaEntity> findByCheckInDateAfter(LocalDate date);
    
    List<BookingJpaEntity> findByCheckOutDateBefore(LocalDate date);
    
    long countByStatus(BookingStatus status);
    
    long countByCustomerId(UUID customerId);
    
    boolean existsByBookingReference(String bookingReference);
    
    @Query("SELECT b FROM BookingJpaEntity b WHERE b.serviceId = :serviceId " +
           "AND b.status NOT IN ('CANCELLED', 'EXPIRED') " +
           "AND ((b.checkInDate <= :checkOut AND b.checkOutDate >= :checkIn) " +
           "OR (b.checkInDate >= :checkIn AND b.checkInDate <= :checkOut))")
    List<BookingJpaEntity> findOverlappingBookings(@Param("serviceId") UUID serviceId,
                                                    @Param("checkIn") LocalDate checkIn,
                                                    @Param("checkOut") LocalDate checkOut);
    
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM BookingJpaEntity b " +
           "WHERE b.serviceId = :serviceId " +
           "AND b.status NOT IN ('CANCELLED', 'EXPIRED') " +
           "AND ((b.checkInDate <= :checkOut AND b.checkOutDate >= :checkIn) " +
           "OR (b.checkInDate >= :checkIn AND b.checkInDate <= :checkOut))")
    boolean hasConflictingBooking(@Param("serviceId") UUID serviceId,
                                   @Param("checkIn") LocalDate checkIn,
                                   @Param("checkOut") LocalDate checkOut);
}
