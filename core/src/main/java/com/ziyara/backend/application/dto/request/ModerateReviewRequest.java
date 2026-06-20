package com.ziyara.backend.application.dto.request;

import com.ziyara.backend.domain.enums.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to moderate a review")
public class ModerateReviewRequest {

    @Schema(description = "New status: PUBLISHED, REJECTED, or HIDDEN")
    private ReviewStatus status;

    @Schema(description = "Reason for rejection")
    private String rejectionReason;
}
