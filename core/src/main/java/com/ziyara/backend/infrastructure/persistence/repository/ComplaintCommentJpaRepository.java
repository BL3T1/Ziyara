package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.ComplaintCommentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository: ComplaintCommentJpaRepository
 */
@Repository
public interface ComplaintCommentJpaRepository extends JpaRepository<ComplaintCommentJpaEntity, UUID> {
    List<ComplaintCommentJpaEntity> findByComplaintId(UUID complaintId);
    List<ComplaintCommentJpaEntity> findByComplaintIdAndIsInternal(UUID complaintId, Boolean isInternal);
}
