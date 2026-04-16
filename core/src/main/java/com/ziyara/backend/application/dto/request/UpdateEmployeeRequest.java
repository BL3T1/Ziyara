package com.ziyara.backend.application.dto.request;

import com.ziyara.backend.domain.enums.EmployeeLevel;
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
@Schema(description = "Request to update employee details")
public class UpdateEmployeeRequest {
    
    @Schema(description = "Updated department ID")
    private UUID departmentId;
    
    @Schema(description = "Updated employee level")
    private EmployeeLevel level;
    
    @Schema(description = "Updated designation")
    private String designation;
}
