package com.ziyarah.infrastructure.persistence.mapper;

import com.ziyarah.domain.entity.TicketComment;
import com.ziyarah.infrastructure.persistence.entity.TicketCommentJpaEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between TicketComment domain entity and JPA entity
 */
@Component
public class TicketCommentMapper {
    
    public TicketComment toDomain(TicketCommentJpaEntity jpaEntity) {
        if (jpaEntity == null) {
            return null;
        }
        
        TicketComment comment = new TicketComment();
        comment.setId(jpaEntity.getId());
        comment.setTicketId(jpaEntity.getTicketId());
        comment.setUserId(jpaEntity.getUserId());
        comment.setComment(jpaEntity.getComment());
        comment.setInternal(jpaEntity.isInternal());
        comment.setResolution(jpaEntity.isResolution());
        comment.setAttachments(jpaEntity.getAttachments());
        comment.setCreatedAt(jpaEntity.getCreatedAt());
        comment.setUpdatedAt(jpaEntity.getUpdatedAt());
        
        return comment;
    }
    
    public TicketCommentJpaEntity toJpaEntity(TicketComment comment) {
        if (comment == null) {
            return null;
        }
        
        TicketCommentJpaEntity jpaEntity = new TicketCommentJpaEntity();
        jpaEntity.setId(comment.getId());
        jpaEntity.setTicketId(comment.getTicketId());
        jpaEntity.setUserId(comment.getUserId());
        jpaEntity.setComment(comment.getComment());
        jpaEntity.setInternal(comment.isInternal());
        jpaEntity.setResolution(comment.isResolution());
        jpaEntity.setAttachments(comment.getAttachments());
        jpaEntity.setCreatedAt(comment.getCreatedAt());
        jpaEntity.setUpdatedAt(comment.getUpdatedAt());
        
        return jpaEntity;
    }
    
    public List<TicketComment> toDomainList(List<TicketCommentJpaEntity> jpaEntities) {
        return jpaEntities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
}
