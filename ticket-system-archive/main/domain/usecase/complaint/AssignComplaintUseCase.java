package com.ziyara.backend.domain.usecase.complaint;

import com.ziyara.backend.domain.entity.Complaint;
import com.ziyara.backend.domain.repository.ComplaintRepository;
import com.ziyara.backend.domain.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

public class AssignComplaintUseCase {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;

    public AssignComplaintUseCase(ComplaintRepository complaintRepository, UserRepository userRepository) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
    }

    public Result execute(Input input) {
        Optional<Complaint> complaintOpt = complaintRepository.findById(input.complaintId());
        if (complaintOpt.isEmpty()) {
            return Result.failure("Complaint not found");
        }

        Complaint complaint = complaintOpt.get();

        if (!complaint.isOpen()) {
            return Result.failure("Complaint is not open for assignment. Current status: " + complaint.getStatus());
        }

        if (!userRepository.existsById(input.agentId())) {
            return Result.failure("Agent not found");
        }

        complaint.assign(input.agentId());

        if (complaint.getAssignedAgentId() == null) {
            return Result.failure("Complaint cannot be assigned in its current state: " + complaint.getStatus());
        }

        Complaint saved = complaintRepository.save(complaint);
        return Result.success(saved);
    }

    public record Input(UUID complaintId, UUID agentId) {}

    public record Result(boolean success, Complaint complaint, String error) {
        public static Result success(Complaint complaint) {
            return new Result(true, complaint, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
