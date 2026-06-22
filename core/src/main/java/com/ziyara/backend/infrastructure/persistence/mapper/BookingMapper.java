package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.Booking;
import com.ziyara.backend.domain.enums.PaymentMethod;
import com.ziyara.backend.infrastructure.persistence.entity.BookingJpaEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
        booking.setRejectionReason(entity.getRejectionReason());
        booking.setDelayReason(entity.getDelayReason());
        booking.setInternalNotes(entity.getInternalNotes());
        booking.setRejectedAt(entity.getRejectedAt());
        booking.setRejectedBy(entity.getRejectedBy());
        booking.setCreatedAt(entity.getCreatedAt());
        booking.setUpdatedAt(entity.getUpdatedAt());
        booking.setDiscountContextMenuItemIds(fromStringList(entity.getDiscountContextMenuItemIds()));
        booking.setDiscountContextMenuSectionIds(fromStringList(entity.getDiscountContextMenuSectionIds()));
        booking.setDiscountContextRoomTypeId(entity.getDiscountContextRoomTypeId());
        booking.setPaymentMethod(entity.getPaymentMethod());
        booking.setPaymentStatus(entity.getPaymentStatus());

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
                .rejectionReason(booking.getRejectionReason())
                .delayReason(booking.getDelayReason())
                .internalNotes(booking.getInternalNotes())
                .rejectedAt(booking.getRejectedAt())
                .rejectedBy(booking.getRejectedBy())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .discountContextMenuItemIds(toStringList(booking.getDiscountContextMenuItemIds()))
                .discountContextMenuSectionIds(toStringList(booking.getDiscountContextMenuSectionIds()))
                .discountContextRoomTypeId(booking.getDiscountContextRoomTypeId())
                .paymentMethod(booking.getPaymentMethod())
                .paymentStatus(booking.getPaymentStatus())
                .build();
    }

    private static List<String> toStringList(List<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return null;
        }
        return uuids.stream().map(UUID::toString).collect(Collectors.toList());
    }

    private static List<UUID> fromStringList(List<String> strings) {
        if (strings == null || strings.isEmpty()) {
            return null;
        }
        return strings.stream().map(UUID::fromString).collect(Collectors.toList());
    }
}
