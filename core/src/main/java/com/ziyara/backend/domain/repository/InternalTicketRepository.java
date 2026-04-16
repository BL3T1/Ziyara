package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.InternalTicket;
import com.ziyara.backend.domain.enums.TicketPriority;
import com.ziyara.backend.domain.enums.TicketStatus;
import com.ziyara.backend.domain.enums.TicketType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for InternalTicket entity
 * Following Clean Architecture - no framework dependencies
 */
public interface InternalTicketRepository {
    
    // Basic CRUD operations
    InternalTicket save(InternalTicket ticket);
    Optional<InternalTicket> findById(UUID id);
    Optional<InternalTicket> findByTicketNumber(String ticketNumber);
    List<InternalTicket> findAll();
    void deleteById(UUID id);
    
    // Find by reporter
    List<InternalTicket> findByReporterId(UUID reporterId);
    
    // Find by assignee
    List<InternalTicket> findByAssignedToId(UUID assignedToId);
    
    // Find by status
    List<InternalTicket> findByStatus(TicketStatus status);
    List<InternalTicket> findByStatusIn(List<TicketStatus> statuses);
    
    // Find by type
    List<InternalTicket> findByType(TicketType type);
    
    // Find by priority
    List<InternalTicket> findByPriority(TicketPriority priority);
    
    // Find by module
    List<InternalTicket> findByModule(String module);
    
    // Find overdue tickets
    List<InternalTicket> findOverdueTickets(LocalDateTime currentDate);
    
    // Find by date range
    List<InternalTicket> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    // Find related tickets
    List<InternalTicket> findByRelatedTicketId(UUID relatedTicketId);
    
    // Count operations
    long count();
    long countByStatus(TicketStatus status);
    long countByType(TicketType type);
    long countByPriority(TicketPriority priority);
    long countByAssignedToId(UUID assignedToId);
    long countOverdueTickets(LocalDateTime currentDate);
    
    // Statistics
    long countByStatusIn(List<TicketStatus> statuses);
    
    // Search
    List<InternalTicket> search(String keyword);
}
