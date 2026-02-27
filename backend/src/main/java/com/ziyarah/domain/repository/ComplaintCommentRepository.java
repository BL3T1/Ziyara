package com.ziyarah.domain.repository;

import com.ziyarah.domain.entity.ComplaintComment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository Port: ComplaintCommentRepository
 */
public interface ComplaintCommentRepository {
    ComplaintComment save(ComplaintComment comment);
    Optional<ComplaintComment> findById(UUID id);
    List<ComplaintComment> findByComplaintId(UUID complaintId);
    List<ComplaintComment> findByComplaintIdAndIsInternal(UUID complaintId, boolean isInternal);
    void deleteById(UUID id);
}
