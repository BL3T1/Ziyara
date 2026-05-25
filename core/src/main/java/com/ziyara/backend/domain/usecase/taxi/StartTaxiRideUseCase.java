package com.ziyara.backend.domain.usecase.taxi;

import com.ziyara.backend.domain.entity.TaxiBooking;
import com.ziyara.backend.domain.repository.TaxiBookingRepository;

import java.util.Optional;
import java.util.UUID;

public class StartTaxiRideUseCase {

    private final TaxiBookingRepository taxiBookingRepository;

    public StartTaxiRideUseCase(TaxiBookingRepository taxiBookingRepository) {
        this.taxiBookingRepository = taxiBookingRepository;
    }

    public Result execute(Input input) {
        Optional<TaxiBooking> taxiOpt = taxiBookingRepository.findById(input.taxiBookingId());
        if (taxiOpt.isEmpty()) {
            return Result.failure("Taxi booking not found");
        }

        TaxiBooking taxi = taxiOpt.get();

        if (!taxi.canStart()) {
            return Result.failure("Taxi ride cannot be started. Current status: " + taxi.getStatus());
        }

        taxi.setDriverId(input.driverId());
        taxi.start();

        TaxiBooking saved = taxiBookingRepository.save(taxi);
        return Result.success(saved);
    }

    public record Input(UUID taxiBookingId, UUID driverId) {}

    public record Result(boolean success, TaxiBooking taxiBooking, String error) {
        public static Result success(TaxiBooking taxiBooking) {
            return new Result(true, taxiBooking, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
