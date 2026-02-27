package com.ziyarah.infrastructure.persistence.adapter;

import com.ziyarah.domain.entity.Booking;
import com.ziyarah.domain.enums.BookingStatus;
import com.ziyarah.domain.repository.BookingRepository;
import com.ziyarah.infrastructure.persistence.entity.BookingJpaEntity;
import com.ziyarah.infrastructure.persistence.mapper.BookingMapper;
import com.ziyarah.infrastructure.persistence.repository.BookingJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository Adapter: BookingRepositoryAdapter
 * Implements domain repository interface using JPA
 * Part of Clean Architecture - Infrastructure Layer
 */
@Component
@RequiredArgsConstructor
public class BookingRepositoryAdapter implements BookingRepository {
    
    private final BookingJpaRepository bookingJpaRepository;
    private final BookingMapper bookingMapper;
    
    @Override
    public Booking save(Booking booking) {
        BookingJpaEntity entity = bookingMapper.toJpaEntity(booking);
        BookingJpaEntity savedEntity = bookingJpaRepository.save(entity);
        return bookingMapper.toDomainEntity(savedEntity);
    }
    
    @Override
    public Optional<Booking> findById(UUID id) {
        return bookingJpaRepository.findById(id)
                .map(bookingMapper::toDomainEntity);
    }
    
    @Override
    public Optional<Booking> findByBookingReference(String bookingReference) {
        return bookingJpaRepository.findByBookingReference(bookingReference)
                .map(bookingMapper::toDomainEntity);
    }
    
    @Override
    public void deleteById(UUID id) {
        bookingJpaRepository.deleteById(id);
    }
    
    @Override
    public void delete(Booking booking) {
        BookingJpaEntity entity = bookingMapper.toJpaEntity(booking);
        bookingJpaRepository.delete(entity);
    }
    
    @Override
    public List<Booking> findAll() {
        return bookingJpaRepository.findAll().stream()
                .map(bookingMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Booking> findByCustomerId(UUID customerId) {
        return bookingJpaRepository.findByCustomerId(customerId).stream()
                .map(bookingMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Booking> findByServiceId(UUID serviceId) {
        return bookingJpaRepository.findByServiceId(serviceId).stream()
                .map(bookingMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Booking> findByStatus(BookingStatus status) {
        return bookingJpaRepository.findByStatus(status).stream()
                .map(bookingMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Booking> findByCustomerIdAndStatus(UUID customerId, BookingStatus status) {
        return bookingJpaRepository.findByCustomerIdAndStatus(customerId, status).stream()
                .map(bookingMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Booking> findByCheckInDateBetween(LocalDate startDate, LocalDate endDate) {
        return bookingJpaRepository.findByCheckInDateBetween(startDate, endDate).stream()
                .map(bookingMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Booking> findByCheckInDateAfter(LocalDate date) {
        return bookingJpaRepository.findByCheckInDateAfter(date).stream()
                .map(bookingMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Booking> findByCheckOutDateBefore(LocalDate date) {
        return bookingJpaRepository.findByCheckOutDateBefore(date).stream()
                .map(bookingMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Booking> findOverlappingBookings(UUID serviceId, LocalDate checkIn, LocalDate checkOut) {
        return bookingJpaRepository.findOverlappingBookings(serviceId, checkIn, checkOut).stream()
                .map(bookingMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean hasConflictingBooking(UUID serviceId, LocalDate checkIn, LocalDate checkOut) {
        return bookingJpaRepository.hasConflictingBooking(serviceId, checkIn, checkOut);
    }
    
    @Override
    public long count() {
        return bookingJpaRepository.count();
    }
    
    @Override
    public long countByStatus(BookingStatus status) {
        return bookingJpaRepository.countByStatus(status);
    }
    
    @Override
    public long countByCustomerId(UUID customerId) {
        return bookingJpaRepository.countByCustomerId(customerId);
    }
    
    @Override
    public boolean existsById(UUID id) {
        return bookingJpaRepository.existsById(id);
    }
    
    @Override
    public boolean existsByBookingReference(String bookingReference) {
        return bookingJpaRepository.existsByBookingReference(bookingReference);
    }
}
