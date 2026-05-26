package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.request.CreateEmployeeRequest;
import com.ziyara.backend.application.dto.request.UpdateEmployeeRequest;
import com.ziyara.backend.application.dto.response.EmployeeResponse;
import com.ziyara.backend.application.service.EmployeeService;
import com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller: EmployeeController
 * Handles internal staff management.
 *
 * DELETE is a soft offboarding — no data is destroyed.
 */
@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@PreAuthorize(ApiAuthorizationExpressions.COMPANY_STAFF)
@Tag(name = "Employees", description = "Internal employee management APIs")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    @Operation(summary = "List employees",
               description = "Returns all employees. Use ?status=ACTIVE to exclude offboarded staff.")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getAllEmployees(
            @Parameter(description = "Filter by status: ACTIVE returns only active employees; "
                                   + "omit or pass ALL to include offboarded records")
            @RequestParam(required = false, defaultValue = "ALL") String status) {

        List<EmployeeResponse> result = "ACTIVE".equalsIgnoreCase(status)
                ? employeeService.getActiveEmployees()
                : employeeService.getAllEmployees();

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get employee", description = "Retrieve employee details by ID")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployee(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getEmployee(id)));
    }

    @PostMapping
    @Operation(summary = "Onboard employee", description = "Add a new employee to the organisation")
    public ResponseEntity<ApiResponse<EmployeeResponse>> onboardEmployee(
            @Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Employee onboarded successfully",
                        employeeService.createEmployee(request)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update employee", description = "Update employee role or department")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Employee details updated",
                employeeService.updateEmployee(id, request)));
    }

    /**
     * Soft-offboards the employee. The record is retained; offboarded_at is set.
     * All historical audit trails remain intact and queryable.
     *
     * @param reason optional free-text reason (passed as query param for simplicity)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Offboard employee",
               description = "Soft-deletes the employee: sets offboarded_at and preserves all "
                           + "historical data and audit trail entries tied to this user ID.")
    public ResponseEntity<ApiResponse<Void>> offboardEmployee(
            @PathVariable UUID id,
            @Parameter(description = "Optional reason for offboarding")
            @RequestParam(required = false) String reason,
            Authentication authentication) {

        UUID actorUserId = resolveActorId(authentication);
        employeeService.offboardEmployee(id, actorUserId, reason);
        return ResponseEntity.ok(ApiResponse.success("Employee offboarded successfully", null));
    }

    // -------------------------------------------------------------------------

    private UUID resolveActorId(Authentication authentication) {
        if (authentication == null) return null;
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ignored) {
            return null; // principal name is email rather than UUID in this deployment
        }
    }
}
