package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.common.PageQuery;
import com.ziyara.backend.domain.common.PagedResult;
import com.ziyara.backend.domain.entity.Review;
import com.ziyara.backend.domain.enums.ReviewStatus;
import com.ziyara.backend.domain.repository.ReviewRepository;
import com.ziyara.backend.infrastructure.persistence.entity.ReviewJpaEntity;
import com.ziyara.backend.infrastructure.persistence.mapper.ReviewMapper;
import com.ziyara.backend.infrastructure.persistence.repository.ReviewJpaRepository;
import com.ziyara.backend.infrastructure.persistence.util.PageConverter;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    public List<Review> findByServiceIdAndStatusIn(UUID serviceId, List<ReviewStatus> statuses) {
        return reviewJpaRepository.findByServiceIdAndStatusIn(serviceId, statuses).stream()
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
    public PagedResult<Review> findAllForAdmin(PageQuery pageQuery, ReviewStatus status, UUID serviceId,
                                               LocalDateTime createdAfterInclusive, LocalDateTime createdBeforeExclusive) {
        Pageable pageable = PageConverter.toPageable(pageQuery);
        Specification<ReviewJpaEntity> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (status != null) {
                p.add(cb.equal(root.get("status"), status));
            }
            if (serviceId != null) {
                p.add(cb.equal(root.get("serviceId"), serviceId));
            }
            if (createdAfterInclusive != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdAfterInclusive));
            }
            if (createdBeforeExclusive != null) {
                p.add(cb.lessThan(root.get("createdAt"), createdBeforeExclusive));
            }
            return p.isEmpty() ? cb.conjunction() : cb.and(p.toArray(Predicate[]::new));
        };
        return PageConverter.toPagedResult(reviewJpaRepository.findAll(spec, pageable), reviewMapper::toDomainEntity);
    }

    @Override
    public void deleteById(UUID id) {
        reviewJpaRepository.deleteById(id);
    }
}
