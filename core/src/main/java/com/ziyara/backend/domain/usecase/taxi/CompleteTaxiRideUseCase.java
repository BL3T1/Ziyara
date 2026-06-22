package com.ziyara.backend.domain.usecase.taxi;

import com.ziyara.backend.domain.entity.TaxiBooking;
import com.ziyara.backend.domain.enums.TaxiStatus;
import com.ziyara.backend.domain.repository.TaxiBookingRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public class CompleteTaxiRideUseCase {

    private final TaxiBookingRepository taxiBookingRepository;

    public CompleteTaxiRideUseCase(TaxiBookingRepository taxiBookingRepository) {
        this.taxiBookingRepository = taxiBookingRepository;
    }

    public Result execute(Input input) {
        Optional<TaxiBooking> taxiOpt = taxiBookingRepository.findById(input.taxiBookingId());
        if (taxiOpt.isEmpty()) {
            return Result.failure("Taxi booking not found");
        }

        TaxiBooking taxi = taxiOpt.get();

        if (taxi.getStatus() != TaxiStatus.IN_PROGRESS) {
            return Result.failure("Only IN_PROGRESS rides can be completed. Current status: " + taxi.getStatus());
        }

        if (input.actualPrice().compareTo(BigDecimal.ZERO) < 0) {
            return Result.failure("Actual price cannot be negative");
        }

        taxi.complete(input.actualPrice(), input.actualDistance());
        TaxiBooking saved = taxiBookingRepository.save(taxi);
        return Result.success(saved);
    }

    public record Input(UUID taxiBookingId, BigDecimal actualPrice, BigDecimal actualDistance) {}

    public record Result(boolean success, TaxiBooking taxiBooking, String error) {
        public static Result success(TaxiBooking taxiBooking) {
            return new Result(true, taxiBooking, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
