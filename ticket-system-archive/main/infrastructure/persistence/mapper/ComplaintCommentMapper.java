package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.ComplaintComment;
import com.ziyara.backend.infrastructure.persistence.entity.ComplaintCommentJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper: ComplaintCommentMapper
 */
@Component
public class ComplaintCommentMapper {
    
    public ComplaintComment toDomainEntity(ComplaintCommentJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        ComplaintComment comment = new ComplaintComment();
        comment.setId(entity.getId());
        comment.setComplaintId(entity.getComplaintId());
        comment.setUserId(entity.getUserId());
        comment.setComment(entity.getComment());
        comment.setInternal(entity.getIsInternal() != null && entity.getIsInternal());
        comment.setCreatedAt(entity.getCreatedAt());
        comment.setUpdatedAt(entity.getUpdatedAt());
        
        return comment;
    }
    
    public ComplaintCommentJpaEntity toJpaEntity(ComplaintComment comment) {
        if (comment == null) {
            return null;
        }
        
        return ComplaintCommentJpaEntity.builder()
                .id(comment.getId())
                .complaintId(comment.getComplaintId())
                .userId(comment.getUserId())
                .comment(comment.getComment())
                .isInternal(comment.isInternal())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
