package com.ziyara.backend.application.dto.request;

import com.ziyara.backend.domain.enums.ComplaintPriority;
import com.ziyara.backend.domain.enums.ComplaintStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update a complaint")
public class UpdateComplaintRequest {

    @Schema(description = "Subject")
    private String subject;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Priority")
    private ComplaintPriority priority;

    @Schema(description = "Status (limited transitions)")
    private ComplaintStatus status;

    @Schema(description = "Category")
    private String category;
}
