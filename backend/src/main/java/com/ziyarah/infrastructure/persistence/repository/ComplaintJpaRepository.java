package com.ziyarah.infrastructure.persistence.repository;

import com.ziyarah.domain.enums.ComplaintPriority;
import com.ziyarah.domain.enums.ComplaintStatus;
import com.ziyarah.infrastructure.persistence.entity.ComplaintJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository: ComplaintJpaRepository
 * Infrastructure layer implementation for Complaint data access
 */
@Repository
public interface ComplaintJpaRepository extends JpaRepository<ComplaintJpaEntity, UUID> {
    
    Optional<ComplaintJpaEntity> findByTicketNumber(String ticketNumber);
    
    List<ComplaintJpaEntity> findByCustomerId(UUID customerId);
    
    List<ComplaintJpaEntity> findByBookingId(UUID bookingId);
    
    List<ComplaintJpaEntity> findByAssignedAgentId(UUID agentId);
    
    List<ComplaintJpaEntity> findByStatus(ComplaintStatus status);
    
    List<ComplaintJpaEntity> findByPriority(ComplaintPriority priority);
    
    List<ComplaintJpaEntity> findByStatusIn(List<ComplaintStatus> statuses);
    
    List<ComplaintJpaEntity> findByCustomerIdAndStatus(UUID customerId, ComplaintStatus status);
    
    List<ComplaintJpaEntity> findByAssignedAgentIdAndStatus(UUID agentId, ComplaintStatus status);
    
    List<ComplaintJpaEntity> findByCreatedAtAfter(LocalDateTime date);
    
    List<ComplaintJpaEntity> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    List<ComplaintJpaEntity> findByResolvedAtBetween(LocalDateTime start, LocalDateTime end);
    
    long countByStatus(ComplaintStatus status);
    
    long countByPriority(ComplaintPriority priority);
    
    long countByAssignedAgentId(UUID agentId);
    
    long countByCustomerId(UUID customerId);
    
    boolean existsByTicketNumber(String ticketNumber);
    
    @Query("SELECT c FROM ComplaintJpaEntity c WHERE c.status NOT IN ('CLOSED', 'REJECTED')")
    List<ComplaintJpaEntity> findOpenComplaints();
    
    @Query("SELECT c FROM ComplaintJpaEntity c WHERE c.assignedAgentId IS NULL " +
           "AND c.status NOT IN ('CLOSED', 'REJECTED')")
    List<ComplaintJpaEntity> findUnassignedComplaints();
    
    @Query("SELECT c FROM ComplaintJpaEntity c WHERE c.status NOT IN ('CLOSED', 'REJECTED') " +
           "AND c.createdAt < :threshold")
    List<ComplaintJpaEntity> findOverdueComplaints(@Param("threshold") LocalDateTime threshold);
}
