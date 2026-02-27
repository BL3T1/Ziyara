package com.ziyarah.domain.repository;

import com.ziyarah.domain.entity.TicketComment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for TicketComment entity
 * Following Clean Architecture - no framework dependencies
 */
public interface TicketCommentRepository {
    
    // Basic CRUD operations
    TicketComment save(TicketComment comment);
    Optional<TicketComment> findById(UUID id);
    List<TicketComment> findAll();
    void deleteById(UUID id);
    
    // Find by ticket
    List<TicketComment> findByTicketId(UUID ticketId);
    List<TicketComment> findByTicketIdOrderByCreatedAtDesc(UUID ticketId);
    
    // Find by user
    List<TicketComment> findByUserId(UUID userId);
    
    // Find resolution comments
    List<TicketComment> findByTicketIdAndIsResolutionTrue(UUID ticketId);
    
    // Find internal/public comments
    List<TicketComment> findByTicketIdAndIsInternalTrue(UUID ticketId);
    List<TicketComment> findByTicketIdAndIsInternalFalse(UUID ticketId);
    
    // Count operations
    long count();
    long countByTicketId(UUID ticketId);
    
    // Delete by ticket
    void deleteByTicketId(UUID ticketId);
}
