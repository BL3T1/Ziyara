package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update a review")
public class UpdateReviewRequest {

    @Min(1)
    @Max(5)
    @Schema(description = "Rating (1-5)")
    private Integer rating;

    @Schema(description = "Comment")
    private String comment;
}
