package com.ziyarah.application.dto.request;

import com.ziyarah.domain.enums.EmployeeLevel;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request to create an employee")
public class CreateEmployeeRequest {
    
    @NotNull(message = "User ID is required")
    @Schema(description = "Associated user ID")
    private UUID userId;
    
    @Schema(description = "Department ID")
    private UUID departmentId;
    
    @NotBlank(message = "Employee ID is required")
    @Schema(description = "Company-specific employee identifier")
    private String employeeId;
    
    @Schema(description = "Employee organizational level")
    private EmployeeLevel level;
    
    @Schema(description = "Job designation")
    private String designation;
}
