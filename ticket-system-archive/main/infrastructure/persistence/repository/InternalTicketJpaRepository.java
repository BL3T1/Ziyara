package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.domain.enums.TicketPriority;
import com.ziyara.backend.domain.enums.TicketStatus;
import com.ziyara.backend.domain.enums.TicketType;
import com.ziyara.backend.infrastructure.persistence.entity.InternalTicketJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for InternalTicket
 * Spring Data JPA interface
 */
@Repository
public interface InternalTicketJpaRepository extends JpaRepository<InternalTicketJpaEntity, UUID> {
    
    Optional<InternalTicketJpaEntity> findByTicketNumber(String ticketNumber);
    
    List<InternalTicketJpaEntity> findByReporterId(UUID reporterId);
    
    List<InternalTicketJpaEntity> findByAssignedToId(UUID assignedToId);
    
    List<InternalTicketJpaEntity> findByStatus(TicketStatus status);
    
    List<InternalTicketJpaEntity> findByStatusIn(List<TicketStatus> statuses);
    
    List<InternalTicketJpaEntity> findByType(TicketType type);
    
    List<InternalTicketJpaEntity> findByPriority(TicketPriority priority);
    
    List<InternalTicketJpaEntity> findByModule(String module);
    
    @Query("SELECT t FROM InternalTicketJpaEntity t WHERE t.dueDate < :currentDate AND t.status NOT IN :closedStatuses")
    List<InternalTicketJpaEntity> findOverdueTickets(@Param("currentDate") LocalDateTime currentDate, 
                                                      @Param("closedStatuses") List<TicketStatus> closedStatuses);
    
    List<InternalTicketJpaEntity> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    List<InternalTicketJpaEntity> findByRelatedTicketId(UUID relatedTicketId);
    
    long countByStatus(TicketStatus status);
    
    long countByType(TicketType type);
    
    long countByPriority(TicketPriority priority);
    
    long countByAssignedToId(UUID assignedToId);
    
    @Query("SELECT COUNT(t) FROM InternalTicketJpaEntity t WHERE t.dueDate < :currentDate AND t.status NOT IN :closedStatuses")
    long countOverdueTickets(@Param("currentDate") LocalDateTime currentDate, 
                             @Param("closedStatuses") List<TicketStatus> closedStatuses);
    
    long countByStatusIn(List<TicketStatus> statuses);
    
    @Query("SELECT t FROM InternalTicketJpaEntity t WHERE " +
           "LOWER(t.subject) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.module) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "t.ticketNumber LIKE CONCAT('%', :keyword, '%')")
    List<InternalTicketJpaEntity> search(@Param("keyword") String keyword);
}
