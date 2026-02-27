package com.ziyarah.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a review")
public class CreateReviewRequest {
    
    @NotNull(message = "Booking ID is required")
    @Schema(description = "Associated booking ID")
    private UUID bookingId;
    
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    @Schema(description = "Rating score (1-5)")
    private Integer rating;
    
    @NotBlank(message = "Comment is required")
    @Schema(description = "Review comment")
    private String comment;
}
