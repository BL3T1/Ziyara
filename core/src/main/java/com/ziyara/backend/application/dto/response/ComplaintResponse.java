package com.ziyara.backend.application.dto.response;

import com.ziyara.backend.domain.enums.ComplaintPriority;
import com.ziyara.backend.domain.enums.ComplaintStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Complaint response")
public class ComplaintResponse {

    @Schema(description = "Complaint ID")
    private UUID id;

    @Schema(description = "Ticket number")
    private String ticketNumber;

    @Schema(description = "Customer ID")
    private UUID customerId;

    @Schema(description = "Booking ID")
    private UUID bookingId;

    @Schema(description = "Subject")
    private String subject;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Priority")
    private ComplaintPriority priority;

    @Schema(description = "Status")
    private ComplaintStatus status;

    @Schema(description = "Category")
    private String category;

    @Schema(description = "Assigned agent ID")
    private UUID assignedAgentId;

    @Schema(description = "Assigned at")
    private LocalDateTime assignedAt;

    @Schema(description = "Resolution notes")
    private String resolutionNotes;

    @Schema(description = "Resolved at")
    private LocalDateTime resolvedAt;

    @Schema(description = "Resolved by")
    private UUID resolvedBy;

    @Schema(description = "Escalated at")
    private LocalDateTime escalatedAt;

    @Schema(description = "Escalated to")
    private UUID escalatedTo;

    @Schema(description = "Closed at")
    private LocalDateTime closedAt;

    @Schema(description = "Created at")
    private LocalDateTime createdAt;

    @Schema(description = "Updated at")
    private LocalDateTime updatedAt;
}
