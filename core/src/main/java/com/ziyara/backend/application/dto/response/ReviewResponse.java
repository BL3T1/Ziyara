package com.ziyara.backend.application.dto.response;

import com.ziyara.backend.domain.enums.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Review details")
public class ReviewResponse {
    
    @Schema(description = "Review ID")
    private UUID id;
    
    @Schema(description = "Booking ID")
    private UUID bookingId;
    
    @Schema(description = "User ID")
    private UUID userId;
    
    @Schema(description = "User display name")
    private String userName;
    
    @Schema(description = "Service ID")
    private UUID serviceId;
    
    @Schema(description = "Rating (1-5)")
    private Integer rating;
    
    @Schema(description = "User comment")
    private String comment;
    
    @Schema(description = "Provider response")
    private String response;
    
    @Schema(description = "Review status")
    private ReviewStatus status;
    
    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;
}
