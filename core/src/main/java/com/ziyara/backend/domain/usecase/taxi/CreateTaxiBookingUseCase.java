package com.ziyara.backend.domain.usecase.taxi;

import com.ziyara.backend.domain.entity.Booking;
import com.ziyara.backend.domain.entity.TaxiBooking;
import com.ziyara.backend.domain.enums.VehicleType;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.TaxiBookingRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class CreateTaxiBookingUseCase {

    private final BookingRepository bookingRepository;
    private final TaxiBookingRepository taxiBookingRepository;

    public CreateTaxiBookingUseCase(BookingRepository bookingRepository,
                                    TaxiBookingRepository taxiBookingRepository) {
        this.bookingRepository = bookingRepository;
        this.taxiBookingRepository = taxiBookingRepository;
    }

    public Result execute(Input input) {
        Optional<Booking> bookingOpt = bookingRepository.findById(input.bookingId());
        if (bookingOpt.isEmpty()) {
            return Result.failure("Booking not found");
        }

        Booking booking = bookingOpt.get();

        if (booking.isCompleted() || booking.getStatus().isFinalState()) {
            return Result.failure("Cannot add taxi to a booking in final state: " + booking.getStatus());
        }

        // Prevent duplicate taxi for the same booking
        Optional<TaxiBooking> existing = taxiBookingRepository.findByBookingId(input.bookingId());
        if (existing.isPresent()) {
            return Result.failure("A taxi booking already exists for this booking");
        }

        TaxiBooking taxi = new TaxiBooking();
        taxi.setBookingId(input.bookingId());
        taxi.setVehicleType(input.vehicleType());
        taxi.setPickupLocation(input.pickupLocation());
        taxi.setDestinationLocation(input.destinationLocation());
        taxi.setPickupLatitude(input.pickupLatitude());
        taxi.setPickupLongitude(input.pickupLongitude());
        taxi.setDestinationLatitude(input.destinationLatitude());
        taxi.setDestinationLongitude(input.destinationLongitude());
        taxi.setScheduledAt(input.scheduledAt());
        taxi.setEstimatedDistance(input.estimatedDistance());
        taxi.setEstimatedPrice(input.estimatedPrice());

        TaxiBooking saved = taxiBookingRepository.save(taxi);
        return Result.success(saved);
    }

    public record Input(
            UUID bookingId,
            VehicleType vehicleType,
            String pickupLocation,
            String destinationLocation,
            double pickupLatitude,
            double pickupLongitude,
            double destinationLatitude,
            double destinationLongitude,
            LocalDateTime scheduledAt,
            BigDecimal estimatedDistance,
            BigDecimal estimatedPrice
    ) {}

    public record Result(boolean success, TaxiBooking taxiBooking, String error) {
        public static Result success(TaxiBooking taxiBooking) {
            return new Result(true, taxiBooking, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
