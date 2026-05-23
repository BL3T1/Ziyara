package com.ziyara.backend.application.dto.response;

import com.ziyara.backend.domain.enums.EmployeeLevel;
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
@Schema(description = "Employee details")
public class EmployeeResponse {
    
    @Schema(description = "Internal ID")
    private UUID id;
    
    @Schema(description = "User ID")
    private UUID userId;
    
    @Schema(description = "Department ID")
    private UUID departmentId;
    
    @Schema(description = "Department name")
    private String departmentName;
    
    @Schema(description = "Employee ID")
    private String employeeId;
    
    @Schema(description = "Employee level")
    private EmployeeLevel level;
    
    @Schema(description = "Employee designation")
    private String designation;
    
    @Schema(description = "Joining date")
    private LocalDateTime joiningDate;

    @Schema(description = "Status: ACTIVE or OFFBOARDED")
    private String status;

    @Schema(description = "Timestamp when the employee was offboarded (null if active)")
    private LocalDateTime offboardedAt;

    @Schema(description = "Reason provided at offboarding (null if active)")
    private String offboardReason;
}
