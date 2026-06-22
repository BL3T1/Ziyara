package com.ziyara.backend.domain.usecase.review;

import com.ziyara.backend.domain.entity.Review;
import com.ziyara.backend.domain.enums.ReviewStatus;
import com.ziyara.backend.domain.repository.ReviewRepository;

import java.util.Optional;
import java.util.UUID;

public class ModerateReviewUseCase {

    private final ReviewRepository reviewRepository;

    public ModerateReviewUseCase(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public Result execute(Input input) {
        Optional<Review> reviewOpt = reviewRepository.findById(input.reviewId());
        if (reviewOpt.isEmpty()) {
            return Result.failure("Review not found");
        }

        Review review = reviewOpt.get();

        if (review.getStatus() != ReviewStatus.PENDING) {
            return Result.failure("Only PENDING reviews can be moderated. Current status: " + review.getStatus());
        }

        if (input.approve()) {
            review.setStatus(ReviewStatus.APPROVED);
        } else {
            if (input.rejectionReason() == null || input.rejectionReason().isBlank()) {
                return Result.failure("A rejection reason is required when rejecting a review");
            }
            review.setStatus(ReviewStatus.REJECTED);
        }

        Review saved = reviewRepository.save(review);
        return Result.success(saved);
    }

    public record Input(UUID reviewId, boolean approve, String rejectionReason, UUID moderatedBy) {}

    public record Result(boolean success, Review review, String error) {
        public static Result success(Review review) {
            return new Result(true, review, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
