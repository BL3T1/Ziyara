package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateReviewRequest;
import com.ziyara.backend.application.dto.response.ReviewResponse;
import com.ziyara.backend.domain.entity.Review;
import com.ziyara.backend.domain.enums.NotificationType;
import com.ziyara.backend.domain.enums.ReviewStatus;
import com.ziyara.backend.domain.repository.ReviewRepository;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationCommandPublisher;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationEvent;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service: ReviewService
 * Handles user reviews and ratings
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final StaffNotificationCommandPublisher staffNotificationCommandPublisher;
    
    @Transactional
    public ReviewResponse createReview(UUID userId, CreateReviewRequest request) {
        log.info("User {} creating review for booking {}", userId, request.getBookingId());
        
        Review review = new Review();
        review.setBookingId(request.getBookingId());
        review.setUserId(userId);
        review.setServiceId(request.getServiceId());
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setStatus(ReviewStatus.PENDING);

        Review saved = reviewRepository.save(review);
        staffNotificationCommandPublisher.publishAfterCommit(StaffNotificationEvent.builder()
                .eventId(UUID.randomUUID())
                .notificationType(NotificationType.SYSTEM_ALERT.name())
                .title("Review pending moderation")
                .message("A new review is pending for booking " + saved.getBookingId())
                .notifyRoles(List.of("SUPPORT_MANAGER"))
                .metadata("{\"reviewId\":\"" + saved.getId() + "\"}")
                .build());
        return mapToResponse(saved);
    }
    
    @Transactional
    public ReviewResponse respondToReview(UUID reviewId, String response) {
        log.info("Responding to review: {}", reviewId);
        
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        
        review.setResponse(response);
        review.setStatus(ReviewStatus.PUBLISHED);
        
        return mapToResponse(reviewRepository.save(review));
    }
    
    @Transactional(readOnly = true)
    public List<ReviewResponse> getServiceReviews(UUID serviceId) {
        return reviewRepository.findByServiceIdAndStatusIn(serviceId, java.util.List.of(ReviewStatus.PUBLISHED, ReviewStatus.APPROVED)).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** Admin: paginated list with optional filters. */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> listAdmin(int page, int size, ReviewStatus status, UUID serviceId,
                                          LocalDate start, LocalDate end) {
        int p = Math.max(0, page);
        int s = Math.min(100, Math.max(1, size));
        Pageable pageable = PageRequest.of(p, s);
        LocalDateTime startDt = start != null ? start.atStartOfDay() : null;
        LocalDateTime endDt = end != null ? end.plusDays(1).atStartOfDay() : null;
        return reviewRepository.findAllForAdmin(pageable, status, serviceId, startDt, endDt)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ReviewResponse getReview(UUID id) {
        return reviewRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
    }

    /** Phase 3: Update review (rating, comment). */
    @Transactional
    public ReviewResponse updateReview(UUID id, com.ziyara.backend.application.dto.request.UpdateReviewRequest request) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        if (request.getRating() != null) review.setRating(request.getRating());
        if (request.getComment() != null) review.setComment(request.getComment());
        return mapToResponse(reviewRepository.save(review));
    }

    /** Phase 3: Delete review. */
    @Transactional
    public void deleteReview(UUID id) {
        if (!reviewRepository.findById(id).isPresent()) {
            throw new ResourceNotFoundException("Review not found");
        }
        reviewRepository.deleteById(id);
    }

    /** Phase 3: Moderate review (set status PUBLISHED, REJECTED, HIDDEN). */
    @Transactional
    public ReviewResponse moderateReview(UUID id, com.ziyara.backend.application.dto.request.ModerateReviewRequest request) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        if (request.getStatus() != null) review.setStatus(request.getStatus());
        return mapToResponse(reviewRepository.save(review));
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .bookingId(review.getBookingId())
                .userId(review.getUserId())
                .serviceId(review.getServiceId())
                .rating(review.getRating())
                .comment(review.getComment())
                .response(review.getResponse())
                .status(review.getStatus())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
