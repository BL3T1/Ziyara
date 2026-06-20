package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.common.PageQuery;
import com.ziyara.backend.domain.common.PagedResult;
import com.ziyara.backend.domain.entity.Booking;
import com.ziyara.backend.domain.enums.BookingStatus;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.infrastructure.persistence.entity.BookingJpaEntity;
import com.ziyara.backend.infrastructure.persistence.mapper.BookingMapper;
import com.ziyara.backend.infrastructure.persistence.repository.BookingJpaRepository;
import com.ziyara.backend.infrastructure.persistence.util.PageConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
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
    public List<Booking> findByServiceIdIn(List<UUID> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) return List.of();
        return bookingJpaRepository.findByServiceIdIn(serviceIds).stream()
                .map(bookingMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public PagedResult<Booking> findFilteredAdmin(BookingStatus status, java.time.LocalDate dateFrom, java.time.LocalDate dateTo, PageQuery pageQuery) {
        Pageable pageable = PageConverter.toPageable(pageQuery);
        return PageConverter.toPagedResult(bookingJpaRepository.findFilteredAdmin(status, dateFrom, dateTo, pageable), bookingMapper::toDomainEntity);
    }

    @Override
    public PagedResult<Booking> findByServiceIdIn(java.util.List<UUID> serviceIds, PageQuery pageQuery) {
        if (serviceIds == null || serviceIds.isEmpty()) return PagedResult.empty(pageQuery);
        Pageable pageable = PageConverter.toPageable(pageQuery);
        return PageConverter.toPagedResult(bookingJpaRepository.findByServiceIdIn(serviceIds, pageable), bookingMapper::toDomainEntity);
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
    public PagedResult<Booking> findAll(PageQuery pageQuery) {
        return PageConverter.toPagedResult(bookingJpaRepository.findAll(PageConverter.toPageable(pageQuery)), bookingMapper::toDomainEntity);
    }

    @Override
    public PagedResult<Booking> findByStatus(BookingStatus status, PageQuery pageQuery) {
        return PageConverter.toPagedResult(bookingJpaRepository.findByStatus(status, PageConverter.toPageable(pageQuery)), bookingMapper::toDomainEntity);
    }

    @Override
    public PagedResult<Booking> findByCustomerId(UUID customerId, PageQuery pageQuery) {
        return PageConverter.toPagedResult(bookingJpaRepository.findByCustomerId(customerId, PageConverter.toPageable(pageQuery)), bookingMapper::toDomainEntity);
    }

    @Override
    public PagedResult<Booking> findByCustomerIdAndStatus(UUID customerId, BookingStatus status, PageQuery pageQuery) {
        return PageConverter.toPagedResult(bookingJpaRepository.findByCustomerIdAndStatus(customerId, status, PageConverter.toPageable(pageQuery)), bookingMapper::toDomainEntity);
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
    public long countByStatusIn(Collection<BookingStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return 0L;
        }
        return bookingJpaRepository.countByStatusIn(statuses);
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
