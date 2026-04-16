package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.Complaint;
import com.ziyara.backend.domain.enums.ComplaintPriority;
import com.ziyara.backend.domain.enums.ComplaintStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository Port: ComplaintRepository
 * Interface for complaint data access - defined in domain layer
 * Implemented by infrastructure layer (Dependency Inversion)
 */
public interface ComplaintRepository {
    
    // CRUD Operations
    Complaint save(Complaint complaint);
    Optional<Complaint> findById(UUID id);
    Optional<Complaint> findByTicketNumber(String ticketNumber);
    void deleteById(UUID id);
    void delete(Complaint complaint);
    
    // Query Operations
    List<Complaint> findAll();
    List<Complaint> findByCustomerId(UUID customerId);
    List<Complaint> findByBookingId(UUID bookingId);
    List<Complaint> findByAssignedAgentId(UUID agentId);
    List<Complaint> findByStatus(ComplaintStatus status);
    List<Complaint> findByPriority(ComplaintPriority priority);
    
    // Filter Operations
    List<Complaint> findByStatusIn(List<ComplaintStatus> statuses);
    List<Complaint> findByCustomerIdAndStatus(UUID customerId, ComplaintStatus status);
    List<Complaint> findByAssignedAgentIdAndStatus(UUID agentId, ComplaintStatus status);
    
    // Time-based queries
    List<Complaint> findByCreatedAtAfter(LocalDateTime date);
    List<Complaint> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<Complaint> findByResolvedAtBetween(LocalDateTime start, LocalDateTime end);
    
    // Open complaints
    List<Complaint> findOpenComplaints();
    List<Complaint> findUnassignedComplaints();
    List<Complaint> findOverdueComplaints(int hoursThreshold);
    
    // Statistics
    long count();
    long countOpenComplaints();
    long countByStatus(ComplaintStatus status);
    long countByPriority(ComplaintPriority priority);
    long countByAssignedAgentId(UUID agentId);
    long countByCustomerId(UUID customerId);
    
    // Existence checks
    boolean existsById(UUID id);
    boolean existsByTicketNumber(String ticketNumber);
}
