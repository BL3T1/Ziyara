package com.ziyarah.application.dto.response;

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
@Schema(description = "Complaint comment details")
public class ComplaintCommentResponse {
    
    @Schema(description = "Comment ID")
    private UUID id;
    
    @Schema(description = "Author user ID")
    private UUID userId;
    
    @Schema(description = "Author name")
    private String userName;
    
    @Schema(description = "Comment content")
    private String comment;
    
    @Schema(description = "Internal status")
    private Boolean isInternal;
    
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;
}
