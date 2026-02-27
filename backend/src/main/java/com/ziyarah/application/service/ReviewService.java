package com.ziyarah.application.service;

import com.ziyarah.application.dto.request.CreateReviewRequest;
import com.ziyarah.application.dto.response.ReviewResponse;
import com.ziyarah.domain.entity.Review;
import com.ziyarah.domain.enums.ReviewStatus;
import com.ziyarah.domain.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    
    @Transactional
    public ReviewResponse createReview(UUID userId, CreateReviewRequest request) {
        log.info("User {} creating review for booking {}", userId, request.getBookingId());
        
        Review review = new Review();
        review.setBookingId(request.getBookingId());
        review.setUserId(userId);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setStatus(ReviewStatus.PENDING);
        
        return mapToResponse(reviewRepository.save(review));
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
        return reviewRepository.findByServiceIdAndStatus(serviceId, ReviewStatus.PUBLISHED).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .bookingId(review.getBookingId())
                .userId(review.getUserId())
                .rating(review.getRating())
                .comment(review.getComment())
                .response(review.getResponse())
                .status(review.getStatus())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
