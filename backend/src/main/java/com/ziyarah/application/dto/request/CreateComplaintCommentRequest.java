package com.ziyarah.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to add a comment to a complaint")
public class CreateComplaintCommentRequest {
    
    @NotBlank(message = "Comment text is required")
    @Schema(description = "Comment content")
    private String comment;
    
    @Schema(description = "Whether the comment is internal staff-only")
    private Boolean isInternal;
}
