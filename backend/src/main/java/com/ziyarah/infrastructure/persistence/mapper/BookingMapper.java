package com.ziyarah.infrastructure.persistence.mapper;

import com.ziyarah.domain.entity.Booking;
import com.ziyarah.infrastructure.persistence.entity.BookingJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper: BookingMapper
 * Maps between domain Booking entity and JPA BookingJpaEntity
 */
@Component
public class BookingMapper {
    
    public Booking toDomainEntity(BookingJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        Booking booking = new Booking();
        booking.setId(entity.getId());
        booking.setBookingReference(entity.getBookingReference());
        booking.setCustomerId(entity.getCustomerId());
        booking.setServiceId(entity.getServiceId());
        booking.setDiscountCodeId(entity.getDiscountCodeId());
        booking.setCheckInDate(entity.getCheckInDate());
        booking.setCheckOutDate(entity.getCheckOutDate());
        booking.setGuests(entity.getGuests() != null ? entity.getGuests() : 1);
        booking.setRooms(entity.getRooms() != null ? entity.getRooms() : 1);
        booking.setBaseAmount(entity.getBaseAmount());
        booking.setDiscountAmount(entity.getDiscountAmount());
        booking.setTaxAmount(entity.getTaxAmount());
        booking.setCommissionAmount(entity.getCommissionAmount());
        booking.setTotalAmount(entity.getTotalAmount());
        booking.setCurrency(entity.getCurrency());
        booking.setStatus(entity.getStatus());
        booking.setSpecialRequests(entity.getSpecialRequests());
        booking.setIdDocumentUrl(entity.getIdDocumentUrl());
        booking.setIdDocumentVerified(entity.getIdDocumentVerified() != null && entity.getIdDocumentVerified());
        booking.setConfirmedAt(entity.getConfirmedAt());
        booking.setCancelledAt(entity.getCancelledAt());
        booking.setCancellationReason(entity.getCancellationReason());
        booking.setCancelledBy(entity.getCancelledBy());
        booking.setCreatedAt(entity.getCreatedAt());
        booking.setUpdatedAt(entity.getUpdatedAt());
        
        return booking;
    }
    
    public BookingJpaEntity toJpaEntity(Booking booking) {
        if (booking == null) {
            return null;
        }
        
        return BookingJpaEntity.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .customerId(booking.getCustomerId())
                .serviceId(booking.getServiceId())
                .discountCodeId(booking.getDiscountCodeId())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .guests(booking.getGuests())
                .rooms(booking.getRooms())
                .baseAmount(booking.getBaseAmount())
                .discountAmount(booking.getDiscountAmount())
                .taxAmount(booking.getTaxAmount())
                .commissionAmount(booking.getCommissionAmount())
                .totalAmount(booking.getTotalAmount())
                .currency(booking.getCurrency())
                .status(booking.getStatus())
                .specialRequests(booking.getSpecialRequests())
                .idDocumentUrl(booking.getIdDocumentUrl())
                .idDocumentVerified(booking.isIdDocumentVerified())
                .confirmedAt(booking.getConfirmedAt())
                .cancelledAt(booking.getCancelledAt())
                .cancellationReason(booking.getCancellationReason())
                .cancelledBy(booking.getCancelledBy())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}
