package com.ziyarah.domain.usecase.booking;

import com.ziyarah.domain.entity.Booking;
import com.ziyarah.domain.enums.BookingStatus;
import com.ziyarah.domain.repository.BookingRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Use Case: Cancel Booking
 * Handles booking cancellation and refund calculation
 * Part of Clean Architecture - Domain Layer
 */
public class CancelBookingUseCase {
    
    private final BookingRepository bookingRepository;
    
    public CancelBookingUseCase(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }
    
    public Result execute(Input input) {
        // Find booking
        Optional<Booking> bookingOpt = bookingRepository.findById(input.bookingId());
        
        if (bookingOpt.isEmpty()) {
            return Result.failure("Booking not found");
        }
        
        Booking booking = bookingOpt.get();
        
        // Verify ownership (if customer is cancelling)
        if (input.customerId() != null && !booking.getCustomerId().equals(input.customerId())) {
            return Result.failure("You are not authorized to cancel this booking");
        }
        
        // Check if booking can be cancelled
        if (!booking.canBeCancelled()) {
            return Result.failure("Booking cannot be cancelled. Current status: " + booking.getStatus());
        }
        
        // Calculate refund
        BigDecimal refundAmount = booking.calculateRefundAmount();
        BigDecimal penaltyAmount = booking.calculatePenaltyAmount();
        
        // Cancel the booking
        booking.cancel(input.cancelledBy(), input.reason());
        
        // Update status to refunding
        booking.setStatus(BookingStatus.REFUNDING);
        
        // Save booking
        Booking savedBooking = bookingRepository.save(booking);
        
        return Result.success(savedBooking, refundAmount, penaltyAmount);
    }
    
    public record Input(UUID bookingId, UUID customerId, UUID cancelledBy, String reason) {}
    
    public record Result(boolean success, Booking booking, BigDecimal refundAmount, 
                         BigDecimal penaltyAmount, String error) {
        public static Result success(Booking booking, BigDecimal refundAmount, BigDecimal penaltyAmount) {
            return new Result(true, booking, refundAmount, penaltyAmount, null);
        }
        
        public static Result failure(String error) {
            return new Result(false, null, BigDecimal.ZERO, BigDecimal.ZERO, error);
        }
    }
}
