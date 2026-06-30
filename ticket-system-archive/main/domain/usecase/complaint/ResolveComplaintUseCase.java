package com.ziyara.backend.domain.usecase.complaint;

import com.ziyara.backend.domain.entity.Complaint;
import com.ziyara.backend.domain.repository.ComplaintRepository;
import java.util.Optional;
import java.util.UUID;

/**
 * Use Case: Resolve Complaint
 * Handles complaint resolution workflow
 * Part of Clean Architecture - Domain Layer
 */
public class ResolveComplaintUseCase {
    
    private final ComplaintRepository complaintRepository;
    
    public ResolveComplaintUseCase(ComplaintRepository complaintRepository) {
        this.complaintRepository = complaintRepository;
    }
    
    public Result execute(Input input) {
        // Find complaint
        Optional<Complaint> complaintOpt = complaintRepository.findById(input.complaintId());
        
        if (complaintOpt.isEmpty()) {
            return Result.failure("Complaint not found");
        }
        
        Complaint complaint = complaintOpt.get();
        
        // Check if complaint can be resolved
        if (!complaint.getStatus().canBeResolved()) {
            return Result.failure("Complaint cannot be resolved. Current status: " + complaint.getStatus());
        }
        
        // Verify agent is assigned
        if (complaint.getAssignedAgentId() == null) {
            return Result.failure("Complaint must be assigned before resolution");
        }
        
        // Resolve the complaint
        complaint.resolve(input.resolvedBy(), input.resolutionNotes());
        
        // Save complaint
        Complaint savedComplaint = complaintRepository.save(complaint);
        
        return Result.success(savedComplaint);
    }
    
    public record Input(UUID complaintId, UUID resolvedBy, String resolutionNotes) {}
    
    public record Result(boolean success, Complaint complaint, String error) {
        public static Result success(Complaint complaint) {
            return new Result(true, complaint, null);
        }
        
        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
