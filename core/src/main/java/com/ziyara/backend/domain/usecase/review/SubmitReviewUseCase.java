package com.ziyara.backend.domain.usecase.review;

import com.ziyara.backend.domain.entity.Booking;
import com.ziyara.backend.domain.entity.Review;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.ReviewRepository;

import java.util.Optional;
import java.util.UUID;

public class SubmitReviewUseCase {

    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;

    public SubmitReviewUseCase(BookingRepository bookingRepository, ReviewRepository reviewRepository) {
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
    }

    public Result execute(Input input) {
        if (input.rating() < 1 || input.rating() > 5) {
            return Result.failure("Rating must be between 1 and 5");
        }

        Optional<Booking> bookingOpt = bookingRepository.findById(input.bookingId());
        if (bookingOpt.isEmpty()) {
            return Result.failure("Booking not found");
        }

        Booking booking = bookingOpt.get();

        if (!booking.getCustomerId().equals(input.userId())) {
            return Result.failure("You can only review your own bookings");
        }

        if (!booking.getStatus().canAcceptReview()) {
            return Result.failure("Booking must be completed before submitting a review. Current status: "
                    + booking.getStatus());
        }

        // One review per booking
        Optional<Review> existing = reviewRepository.findByBookingId(input.bookingId());
        if (existing.isPresent()) {
            return Result.failure("A review has already been submitted for this booking");
        }

        Review review = new Review();
        review.setBookingId(input.bookingId());
        review.setUserId(input.userId());
        review.setServiceId(booking.getServiceId());
        review.setRating(input.rating());
        review.setComment(input.comment());

        Review saved = reviewRepository.save(review);
        return Result.success(saved);
    }

    public record Input(UUID bookingId, UUID userId, int rating, String comment) {}

    public record Result(boolean success, Review review, String error) {
        public static Result success(Review review) {
            return new Result(true, review, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
