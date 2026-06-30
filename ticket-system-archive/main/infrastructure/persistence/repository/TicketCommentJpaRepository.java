package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.TicketCommentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for TicketComment
 * Spring Data JPA interface
 */
@Repository
public interface TicketCommentJpaRepository extends JpaRepository<TicketCommentJpaEntity, UUID> {
    
    List<TicketCommentJpaEntity> findByTicketId(UUID ticketId);
    
    List<TicketCommentJpaEntity> findByTicketIdOrderByCreatedAtDesc(UUID ticketId);
    
    List<TicketCommentJpaEntity> findByUserId(UUID userId);
    
    List<TicketCommentJpaEntity> findByTicketIdAndIsResolutionTrue(UUID ticketId);
    
    List<TicketCommentJpaEntity> findByTicketIdAndIsInternalTrue(UUID ticketId);
    
    List<TicketCommentJpaEntity> findByTicketIdAndIsInternalFalse(UUID ticketId);
    
    long countByTicketId(UUID ticketId);
    
    void deleteByTicketId(UUID ticketId);
}
