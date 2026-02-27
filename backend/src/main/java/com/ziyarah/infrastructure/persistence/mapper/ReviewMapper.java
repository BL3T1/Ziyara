package com.ziyarah.infrastructure.persistence.mapper;

import com.ziyarah.domain.entity.Review;
import com.ziyarah.infrastructure.persistence.entity.ReviewJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper: ReviewMapper
 */
@Component
public class ReviewMapper {
    
    public Review toDomainEntity(ReviewJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        Review review = new Review();
        review.setId(entity.getId());
        review.setBookingId(entity.getBookingId());
        review.setUserId(entity.getUserId());
        review.setServiceId(entity.getServiceId());
        review.setRating(entity.getRating() != null ? entity.getRating() : 0);
        review.setComment(entity.getComment());
        review.setResponse(entity.getResponse());
        review.setStatus(entity.getStatus());
        review.setCreatedAt(entity.getCreatedAt());
        review.setUpdatedAt(entity.getUpdatedAt());
        
        return review;
    }
    
    public ReviewJpaEntity toJpaEntity(Review review) {
        if (review == null) {
            return null;
        }
        
        return ReviewJpaEntity.builder()
                .id(review.getId())
                .bookingId(review.getBookingId())
                .userId(review.getUserId())
                .serviceId(review.getServiceId())
                .rating(review.getRating())
                .comment(review.getComment())
                .response(review.getResponse())
                .status(review.getStatus())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
