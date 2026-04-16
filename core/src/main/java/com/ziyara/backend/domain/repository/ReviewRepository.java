package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.Review;
import com.ziyara.backend.domain.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository Port: ReviewRepository
 */
public interface ReviewRepository {
    Review save(Review review);
    Optional<Review> findById(UUID id);
    Optional<Review> findByBookingId(UUID bookingId);
    List<Review> findByServiceId(UUID serviceId);
    List<Review> findByServiceIdAndStatus(UUID serviceId, ReviewStatus status);
    List<Review> findByServiceIdAndStatusIn(UUID serviceId, List<ReviewStatus> statuses);
    List<Review> findByStatus(ReviewStatus status);

    /**
     * Admin list with optional filters. {@code createdBefore} is exclusive (use end date + 1 day at start of day).
     */
    Page<Review> findAllForAdmin(Pageable pageable, ReviewStatus status, UUID serviceId,
                                  LocalDateTime createdAfterInclusive, LocalDateTime createdBeforeExclusive);

    void deleteById(UUID id);
}
