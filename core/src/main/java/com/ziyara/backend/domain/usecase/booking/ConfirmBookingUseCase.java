package com.ziyara.backend.domain.usecase.booking;

import com.ziyara.backend.domain.entity.Booking;
import com.ziyara.backend.domain.enums.BookingStatus;
import com.ziyara.backend.domain.repository.BookingRepository;

import java.util.Optional;
import java.util.UUID;

public class ConfirmBookingUseCase {

    private final BookingRepository bookingRepository;

    public ConfirmBookingUseCase(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public Result execute(Input input) {
        Optional<Booking> bookingOpt = bookingRepository.findById(input.bookingId());
        if (bookingOpt.isEmpty()) {
            return Result.failure("Booking not found");
        }

        Booking booking = bookingOpt.get();

        if (!booking.isPending()) {
            return Result.failure("Only PENDING bookings can be confirmed. Current status: " + booking.getStatus());
        }

        booking.confirm();
        Booking saved = bookingRepository.save(booking);
        return Result.success(saved);
    }

    public record Input(UUID bookingId, UUID confirmedBy) {}

    public record Result(boolean success, Booking booking, String error) {
        public static Result success(Booking booking) {
            return new Result(true, booking, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
