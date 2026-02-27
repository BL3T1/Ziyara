package com.ziyarah.domain.repository;

import com.ziyarah.domain.entity.Review;
import com.ziyarah.domain.enums.ReviewStatus;
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
    List<Review> findByStatus(ReviewStatus status);
    void deleteById(UUID id);
}
