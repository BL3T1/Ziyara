package com.ziyarah.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Department details")
public class DepartmentResponse {
    
    @Schema(description = "Department ID")
    private UUID id;
    
    @Schema(description = "Department name")
    private String name;
    
    @Schema(description = "Department description")
    private String description;
    
    @Schema(description = "Manager user ID")
    private UUID managerId;
}
