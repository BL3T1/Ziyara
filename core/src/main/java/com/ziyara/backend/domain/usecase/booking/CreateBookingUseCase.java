package com.ziyara.backend.domain.usecase.booking;

import com.ziyara.backend.domain.entity.Booking;
import com.ziyara.backend.domain.entity.Service;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.ServiceRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class CreateBookingUseCase {

    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;

    public CreateBookingUseCase(BookingRepository bookingRepository, ServiceRepository serviceRepository) {
        this.bookingRepository = bookingRepository;
        this.serviceRepository = serviceRepository;
    }

    public Result execute(Input input) {
        Optional<Service> serviceOpt = serviceRepository.findById(input.serviceId());
        if (serviceOpt.isEmpty()) {
            return Result.failure("Service not found");
        }

        Service service = serviceOpt.get();

        if (!service.isAvailable()) {
            return Result.failure("Service is not available for booking");
        }

        if (!service.hasRoomsAvailable(input.rooms())) {
            return Result.failure("Not enough rooms available. Requested: " + input.rooms()
                    + ", available: " + service.getAvailableRooms());
        }

        if (input.checkOutDate() != null && !input.checkOutDate().isAfter(input.checkInDate())) {
            return Result.failure("Check-out date must be after check-in date");
        }

        if (input.checkInDate().isBefore(LocalDate.now())) {
            return Result.failure("Check-in date cannot be in the past");
        }

        Booking booking = new Booking();
        booking.setCustomerId(input.customerId());
        booking.setServiceId(input.serviceId());
        booking.setCheckInDate(input.checkInDate());
        booking.setCheckOutDate(input.checkOutDate());
        booking.setGuests(input.guests());
        booking.setRooms(input.rooms());
        booking.setBaseAmount(input.baseAmount());
        booking.setDiscountAmount(input.discountAmount());
        booking.setTaxAmount(input.taxAmount());
        booking.setTotalAmount(input.totalAmount());
        booking.setCurrency(input.currency());
        booking.setSpecialRequests(input.specialRequests());
        booking.setDiscountCodeId(input.discountCodeId());
        booking.setBookingReference(generateReference());

        service.reduceAvailability(input.rooms());
        serviceRepository.save(service);

        Booking savedBooking = bookingRepository.save(booking);
        return Result.success(savedBooking);
    }

    private String generateReference() {
        return "ZYR-" + System.currentTimeMillis();
    }

    public record Input(
            UUID customerId,
            UUID serviceId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int guests,
            int rooms,
            BigDecimal baseAmount,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            String currency,
            String specialRequests,
            UUID discountCodeId
    ) {}

    public record Result(boolean success, Booking booking, String error) {
        public static Result success(Booking booking) {
            return new Result(true, booking, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
