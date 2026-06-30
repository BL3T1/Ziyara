package com.ziyara.backend.domain.usecase.complaint;

import com.ziyara.backend.domain.entity.Complaint;
import com.ziyara.backend.domain.enums.ComplaintPriority;
import com.ziyara.backend.domain.repository.ComplaintRepository;
import com.ziyara.backend.domain.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

public class SubmitComplaintUseCase {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;

    public SubmitComplaintUseCase(ComplaintRepository complaintRepository, UserRepository userRepository) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
    }

    public Result execute(Input input) {
        if (input.subject() == null || input.subject().isBlank()) {
            return Result.failure("Complaint subject is required");
        }

        if (input.description() == null || input.description().isBlank()) {
            return Result.failure("Complaint description is required");
        }

        if (!userRepository.existsById(input.customerId())) {
            return Result.failure("Customer not found");
        }

        Complaint complaint = new Complaint(input.customerId(), input.subject(), input.description());
        complaint.setBookingId(input.bookingId());
        complaint.setCategory(input.category());
        complaint.setPriority(input.priority() != null ? input.priority() : ComplaintPriority.MEDIUM);
        complaint.setTicketNumber(generateTicketNumber());

        Complaint saved = complaintRepository.save(complaint);
        return Result.success(saved);
    }

    private String generateTicketNumber() {
        return "TCK-" + System.currentTimeMillis();
    }

    public record Input(
            UUID customerId,
            UUID bookingId,
            String subject,
            String description,
            String category,
            ComplaintPriority priority
    ) {}

    public record Result(boolean success, Complaint complaint, String error) {
        public static Result success(Complaint complaint) {
            return new Result(true, complaint, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
