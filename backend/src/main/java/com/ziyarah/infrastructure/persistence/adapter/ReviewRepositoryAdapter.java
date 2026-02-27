package com.ziyarah.infrastructure.persistence.adapter;

import com.ziyarah.domain.entity.Review;
import com.ziyarah.domain.enums.ReviewStatus;
import com.ziyarah.domain.repository.ReviewRepository;
import com.ziyarah.infrastructure.persistence.entity.ReviewJpaEntity;
import com.ziyarah.infrastructure.persistence.mapper.ReviewMapper;
import com.ziyarah.infrastructure.persistence.repository.ReviewJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository Adapter: ReviewRepositoryAdapter
 */
@Component
@RequiredArgsConstructor
public class ReviewRepositoryAdapter implements ReviewRepository {
    
    private final ReviewJpaRepository reviewJpaRepository;
    private final ReviewMapper reviewMapper;
    
    @Override
    public Review save(Review review) {
        ReviewJpaEntity entity = reviewMapper.toJpaEntity(review);
        ReviewJpaEntity savedEntity = reviewJpaRepository.save(entity);
        return reviewMapper.toDomainEntity(savedEntity);
    }
    
    @Override
    public Optional<Review> findById(UUID id) {
        return reviewJpaRepository.findById(id)
                .map(reviewMapper::toDomainEntity);
    }
    
    @Override
    public Optional<Review> findByBookingId(UUID bookingId) {
        return reviewJpaRepository.findByBookingId(bookingId)
                .map(reviewMapper::toDomainEntity);
    }
    
    @Override
    public List<Review> findByServiceId(UUID serviceId) {
        return reviewJpaRepository.findByServiceId(serviceId).stream()
                .map(reviewMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Review> findByServiceIdAndStatus(UUID serviceId, ReviewStatus status) {
        return reviewJpaRepository.findByServiceIdAndStatus(serviceId, status).stream()
                .map(reviewMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Review> findByStatus(ReviewStatus status) {
        return reviewJpaRepository.findAll().stream() // Simplified, should have finding by status in JPA repo if needed
                .map(reviewMapper::toDomainEntity)
                .filter(r -> r.getStatus() == status)
                .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(UUID id) {
        reviewJpaRepository.deleteById(id);
    }
}
