package com.ziyara.backend.domain.usecase.booking;

import com.ziyara.backend.domain.entity.Booking;
import com.ziyara.backend.domain.entity.Service;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.ServiceRepository;

import java.util.Optional;
import java.util.UUID;

public class ExpireBookingUseCase {

    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;

    public ExpireBookingUseCase(BookingRepository bookingRepository, ServiceRepository serviceRepository) {
        this.bookingRepository = bookingRepository;
        this.serviceRepository = serviceRepository;
    }

    public Result execute(Input input) {
        Optional<Booking> bookingOpt = bookingRepository.findById(input.bookingId());
        if (bookingOpt.isEmpty()) {
            return Result.failure("Booking not found");
        }

        Booking booking = bookingOpt.get();

        if (!booking.isPending()) {
            return Result.failure("Only PENDING bookings can be expired. Current status: " + booking.getStatus());
        }

        booking.expire();
        Booking saved = bookingRepository.save(booking);

        // Restore availability to allow other bookings
        serviceRepository.findById(booking.getServiceId()).ifPresent(service -> {
            service.increaseAvailability(booking.getRooms());
            serviceRepository.save(service);
        });

        return Result.success(saved);
    }

    public record Input(UUID bookingId) {}

    public record Result(boolean success, Booking booking, String error) {
        public static Result success(Booking booking) {
            return new Result(true, booking, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
