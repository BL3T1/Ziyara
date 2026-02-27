package com.ziyarah.domain.repository;

import com.ziyarah.domain.entity.Booking;
import com.ziyarah.domain.enums.BookingStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository Port: BookingRepository
 * Interface for booking data access - defined in domain layer
 * Implemented by infrastructure layer (Dependency Inversion)
 */
public interface BookingRepository {
    
    // CRUD Operations
    Booking save(Booking booking);
    Optional<Booking> findById(UUID id);
    Optional<Booking> findByBookingReference(String bookingReference);
    void deleteById(UUID id);
    void delete(Booking booking);
    
    // Query Operations
    List<Booking> findAll();
    List<Booking> findByCustomerId(UUID customerId);
    List<Booking> findByServiceId(UUID serviceId);
    List<Booking> findByStatus(BookingStatus status);
    List<Booking> findByCustomerIdAndStatus(UUID customerId, BookingStatus status);
    
    // Date-based queries
    List<Booking> findByCheckInDateBetween(LocalDate startDate, LocalDate endDate);
    List<Booking> findByCheckInDateAfter(LocalDate date);
    List<Booking> findByCheckOutDateBefore(LocalDate date);
    
    // Availability check
    List<Booking> findOverlappingBookings(UUID serviceId, LocalDate checkIn, LocalDate checkOut);
    boolean hasConflictingBooking(UUID serviceId, LocalDate checkIn, LocalDate checkOut);
    
    // Statistics
    long count();
    long countByStatus(BookingStatus status);
    long countByCustomerId(UUID customerId);
    
    // Existence checks
    boolean existsById(UUID id);
    boolean existsByBookingReference(String bookingReference);
}
