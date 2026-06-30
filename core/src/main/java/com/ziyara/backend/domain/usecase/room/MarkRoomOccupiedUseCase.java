package com.ziyara.backend.domain.usecase.room;

import com.ziyara.backend.domain.entity.WalkInOccupation;
import com.ziyara.backend.domain.enums.BookingStatus;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.WalkInOccupationRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Marks a room as walk-in occupied for a date range.
 * Cancels any PENDING or CONFIRMED online bookings that overlap.
 * Writes an audit record to hotel_walk_in_occupations.
 * Notification/email delivery is handled by the calling service after this use case returns.
 */
public class MarkRoomOccupiedUseCase {

    private final BookingRepository bookingRepository;
    private final WalkInOccupationRepository walkInOccupationRepository;

    public MarkRoomOccupiedUseCase(BookingRepository bookingRepository,
                                    WalkInOccupationRepository walkInOccupationRepository) {
        this.bookingRepository = bookingRepository;
        this.walkInOccupationRepository = walkInOccupationRepository;
    }

    public Result execute(Input input) {
        if (!input.checkOut().isAfter(input.checkIn())) {
            return Result.failure("checkOutDate must be after checkInDate");
        }

        List<UUID> cancelledBookingIds = bookingRepository
                .findConflictingByRoomId(input.roomId(), input.checkIn(), input.checkOut())
                .stream()
                .map(booking -> {
                    booking.setStatus(BookingStatus.CANCELLED);
                    booking.setCancellationReason("Walk-in guest: " + input.reason());
                    booking.setCancelledBy(input.recordedBy());
                    bookingRepository.save(booking);
                    return booking.getId();
                })
                .toList();

        WalkInOccupation occupation = new WalkInOccupation();
        occupation.setRoomId(input.roomId());
        occupation.setServiceId(input.serviceId());
        occupation.setProviderId(input.providerId());
        occupation.setCheckInDate(input.checkIn());
        occupation.setCheckOutDate(input.checkOut());
        occupation.setReason(input.reason());
        occupation.setRecordedBy(input.recordedBy());
        walkInOccupationRepository.save(occupation);

        return Result.success(cancelledBookingIds);
    }

    public record Input(
            UUID roomId,
            UUID serviceId,
            UUID providerId,
            LocalDate checkIn,
            LocalDate checkOut,
            String reason,
            UUID recordedBy
    ) {}

    public record Result(boolean success, List<UUID> cancelledBookingIds, String error) {
        public static Result success(List<UUID> cancelled) {
            return new Result(true, cancelled, null);
        }
        public static Result failure(String error) {
            return new Result(false, List.of(), error);
        }
    }
}
