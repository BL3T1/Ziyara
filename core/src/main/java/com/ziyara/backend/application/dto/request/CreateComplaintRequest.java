package com.ziyara.backend.application.dto.request;

import com.ziyara.backend.domain.enums.ComplaintPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to create a complaint")
public class CreateComplaintRequest {

    @NotNull
    @Schema(description = "Customer (user) ID", required = true)
    private UUID customerId;

    @Schema(description = "Optional booking ID")
    private UUID bookingId;

    @NotBlank
    @Schema(description = "Subject", required = true)
    private String subject;

    @NotBlank
    @Schema(description = "Description", required = true)
    private String description;

    @Schema(description = "Priority")
    private ComplaintPriority priority;

    @Schema(description = "Category")
    private String category;
}
