package com.ziyarah.presentation.controller;

import com.ziyarah.application.dto.ApiResponse;
import com.ziyarah.application.dto.request.CreateEmployeeRequest;
import com.ziyarah.application.dto.request.UpdateEmployeeRequest;
import com.ziyarah.application.dto.response.EmployeeResponse;
import com.ziyarah.application.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller: EmployeeController
 * Handles internal staff management
 */
@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@Tag(name = "Employees", description = "Internal employee management APIs")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeController {
    
    private final EmployeeService employeeService;
    
    @GetMapping
    @Operation(summary = "List employees", description = "Retrieve all company employees")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getAllEmployees() {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getAllEmployees()));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get employee", description = "Retrieve employee details by ID")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployee(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getEmployee(id)));
    }
    
    @PostMapping
    @Operation(summary = "Onboard employee", description = "Add a new employee to the organization")
    public ResponseEntity<ApiResponse<EmployeeResponse>> onboardEmployee(
            @Valid @RequestBody CreateEmployeeRequest request
    ) {
        EmployeeResponse response = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Employee onboarded successfully", response));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update employee", description = "Update employee role or department")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEmployeeRequest request
    ) {
        EmployeeResponse response = employeeService.updateEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.success("Employee details updated", response));
    }
}
