package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Approve or reject a media submission")
public class ReviewMediaSubmissionRequest {
    @Schema(description = "Optional note for rejection reason")
    private String note;
}
