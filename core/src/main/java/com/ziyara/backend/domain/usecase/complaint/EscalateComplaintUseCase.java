package com.ziyara.backend.domain.usecase.complaint;

import com.ziyara.backend.domain.entity.Complaint;
import com.ziyara.backend.domain.enums.ComplaintStatus;
import com.ziyara.backend.domain.repository.ComplaintRepository;

import java.util.Optional;
import java.util.UUID;

public class EscalateComplaintUseCase {

    private final ComplaintRepository complaintRepository;

    public EscalateComplaintUseCase(ComplaintRepository complaintRepository) {
        this.complaintRepository = complaintRepository;
    }

    public Result execute(Input input) {
        Optional<Complaint> complaintOpt = complaintRepository.findById(input.complaintId());
        if (complaintOpt.isEmpty()) {
            return Result.failure("Complaint not found");
        }

        Complaint complaint = complaintOpt.get();

        if (complaint.getStatus() != ComplaintStatus.IN_PROGRESS
                && complaint.getStatus() != ComplaintStatus.ASSIGNED) {
            return Result.failure("Only IN_PROGRESS or ASSIGNED complaints can be escalated. Current status: "
                    + complaint.getStatus());
        }

        complaint.escalate(input.escalateToId());
        Complaint saved = complaintRepository.save(complaint);
        return Result.success(saved);
    }

    public record Input(UUID complaintId, UUID escalateToId) {}

    public record Result(boolean success, Complaint complaint, String error) {
        public static Result success(Complaint complaint) {
            return new Result(true, complaint, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
