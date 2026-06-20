package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.response.DepartmentResponse;
import com.ziyara.backend.application.service.DepartmentService;
import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize(COMPANY_STAFF)
@Tag(name = "Departments", description = "Department management APIs")
@SecurityRequirement(name = "bearerAuth")
public class DepartmentController {
    
    private final DepartmentService departmentService;
    
    @GetMapping
    @Operation(summary = "List departments", description = "Retrieve all organization departments")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getAllDepartments() {
        return ResponseEntity.ok(ApiResponse.success(departmentService.getAllDepartments()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get department", description = "Retrieve department by ID")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getDepartment(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(departmentService.getDepartment(id)));
    }
    
    @PatchMapping("/{id}")
    @Operation(summary = "Update department", description = "Update department name, description, or manager")
    public ResponseEntity<ApiResponse<DepartmentResponse>> updateDepartment(
            @PathVariable UUID id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) UUID managerId) {
        return ResponseEntity.ok(ApiResponse.success(
                departmentService.updateDepartment(id, name, description, managerId)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete department", description = "Delete department (fails if employees are assigned)")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable UUID id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok(ApiResponse.success("Department deleted", null));
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
