package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateReviewRequest;
import com.ziyara.backend.application.dto.response.ReviewResponse;
import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.application.exception.UnauthorizedException;
import com.ziyara.backend.domain.entity.Review;
import com.ziyara.backend.domain.enums.NotificationType;
import com.ziyara.backend.domain.enums.ReviewStatus;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.ReviewRepository;
import com.ziyara.backend.domain.usecase.review.ModerateReviewUseCase;
import com.ziyara.backend.domain.usecase.review.SubmitReviewUseCase;
import com.ziyara.backend.modules.notification.api.StaffNotificationCommandPublisher;
import com.ziyara.backend.application.dto.StaffNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ziyara.backend.domain.common.PageQuery;
import com.ziyara.backend.infrastructure.persistence.util.PageConverter;
import org.springframework.data.domain.Page;
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
    private final BookingRepository bookingRepository;
    private final StaffNotificationCommandPublisher staffNotificationCommandPublisher;
    
    @Transactional
    public ReviewResponse createReview(UUID userId, CreateReviewRequest request) {
        log.info("User {} creating review for booking {}", userId, request.getBookingId());

        var result = new SubmitReviewUseCase(bookingRepository, reviewRepository).execute(
                new SubmitReviewUseCase.Input(request.getBookingId(), userId,
                        request.getRating(), request.getComment()));
        if (!result.success()) throw new BusinessException(result.error());
        Review saved = result.review();
        staffNotificationCommandPublisher.publishAfterCommit(StaffNotificationEvent.builder()
                .eventId(UUID.randomUUID())
                .notificationType(NotificationType.SYSTEM_ALERT.name())
                .title("Review pending moderation")
                .message("A new review is pending for booking " + saved.getBookingId())
                .notifyRoles(List.of("SUPPORT_MANAGER", "SUPPORT_AGENT"))
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
        PageQuery query = PageQuery.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        LocalDateTime startDt = start != null ? start.atStartOfDay() : null;
        LocalDateTime endDt = end != null ? end.plusDays(1).atStartOfDay() : null;
        return PageConverter.toSpringPage(
                reviewRepository.findAllForAdmin(query, status, serviceId, startDt, endDt), query, this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ReviewResponse getReview(UUID id) {
        return reviewRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
    }

    /** Update review — only the author or company staff may edit. */
    @Transactional
    public ReviewResponse updateReview(UUID id, com.ziyara.backend.application.dto.request.UpdateReviewRequest request,
                                       UUID requestingUserId, boolean isStaff) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        assertCanModify(review, requestingUserId, isStaff);
        if (request.getRating() != null) review.setRating(request.getRating());
        if (request.getComment() != null) review.setComment(request.getComment());
        return mapToResponse(reviewRepository.save(review));
    }

    /** Delete review — only the author or company staff may delete. */
    @Transactional
    public void deleteReview(UUID id, UUID requestingUserId, boolean isStaff) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        assertCanModify(review, requestingUserId, isStaff);
        reviewRepository.deleteById(id);
    }

    /** Phase 3: Moderate review (approve or reject). */
    @Transactional
    public ReviewResponse moderateReview(UUID id, com.ziyara.backend.application.dto.request.ModerateReviewRequest request) {
        boolean approve = request.getStatus() == ReviewStatus.APPROVED || request.getStatus() == ReviewStatus.PUBLISHED;
        var result = new ModerateReviewUseCase(reviewRepository).execute(
                new ModerateReviewUseCase.Input(id, approve, request.getRejectionReason(), null));
        if (!result.success()) throw new BusinessException(result.error());
        return mapToResponse(result.review());
    }

    private void assertCanModify(Review review, UUID requestingUserId, boolean isStaff) {
        if (!isStaff && !review.getUserId().equals(requestingUserId)) {
            throw new UnauthorizedException("You don't have access to this review");
        }
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
