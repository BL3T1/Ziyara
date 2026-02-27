package com.ziyarah.presentation.controller;

import com.ziyarah.application.dto.ApiResponse;
import com.ziyarah.application.dto.response.DepartmentResponse;
import com.ziyarah.application.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller: DepartmentController
 * Handles organizational unit management
 */
@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
@Tag(name = "Departments", description = "Department management APIs")
@SecurityRequirement(name = "bearerAuth")
public class DepartmentController {
    
    private final DepartmentService departmentService;
    
    @GetMapping
    @Operation(summary = "List departments", description = "Retrieve all organization departments")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getAllDepartments() {
        return ResponseEntity.ok(ApiResponse.success(departmentService.getAllDepartments()));
    }
    
    @PostMapping
    @Operation(summary = "Create department", description = "Add a new department (Admin only)")
    public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartment(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) UUID managerId
    ) {
        DepartmentResponse response = departmentService.createDepartment(name, description, managerId);
        return ResponseEntity.ok(ApiResponse.success("Department created", response));
    }
}
