package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateEmployeeRequest;
import com.ziyara.backend.application.dto.request.UpdateEmployeeRequest;
import com.ziyara.backend.application.dto.response.EmployeeResponse;
import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.domain.entity.Employee;
import com.ziyara.backend.domain.repository.EmployeeRepository;
import com.ziyara.backend.domain.usecase.employee.OffboardEmployeeUseCase;
import com.ziyara.backend.domain.usecase.employee.OnboardEmployeeUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service: EmployeeService
 * Handles internal employee lifecycle. Deletions are soft (offboarded_at is set)
 * so all historical records and audit trails are preserved against the user ID.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployee(UUID id) {
        return employeeRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
    }

    /**
     * Returns all employees including offboarded ones.
     * Use {@code status} query param in the controller to filter by ACTIVE/OFFBOARDED.
     */
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getActiveEmployees() {
        return employeeRepository.findAllActive().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        log.info("Creating employee for user: {}", request.getUserId());
        var result = new OnboardEmployeeUseCase(employeeRepository)
                .execute(new OnboardEmployeeUseCase.Input(
                        request.getUserId(),
                        request.getDepartmentId(),
                        request.getEmployeeId(),
                        request.getLevel(),
                        request.getDesignation()
                ));
        if (!result.success()) throw new BusinessException(result.error());
        return mapToResponse(result.employee());
    }

    @Transactional
    public EmployeeResponse updateEmployee(UUID id, UpdateEmployeeRequest request) {
        log.info("Updating employee: {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (employee.isOffboarded()) {
            throw new BusinessException("Cannot update an offboarded employee");
        }

        if (request.getDepartmentId() != null) employee.setDepartmentId(request.getDepartmentId());
        if (request.getLevel()        != null) employee.setLevel(request.getLevel());
        if (request.getDesignation()  != null) employee.setDesignation(request.getDesignation());

        return mapToResponse(employeeRepository.save(employee));
    }

    /**
     * Soft-offboards an employee: sets offboarded_at and offboard_reason.
     * The employee record and ALL associated audit trail entries are preserved.
     * No cascade — the linked sys_users row is untouched.
     *
     * @param id          employee ID (== user_id in this schema)
     * @param actorUserId ID of the manager performing the action (for audit)
     * @param reason      optional reason for the offboarding
     */
    @Transactional
    public void offboardEmployee(UUID id, UUID actorUserId, String reason) {
        var result = new OffboardEmployeeUseCase(employeeRepository)
                .execute(new OffboardEmployeeUseCase.Input(id, actorUserId, reason));
        if (!result.success()) {
            if ("Employee not found".equals(result.error())) throw new ResourceNotFoundException(result.error());
            throw new BusinessException(result.error());
        }
        log.info("Employee {} soft-offboarded by {}, reason: {}", id, actorUserId, reason);
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private EmployeeResponse mapToResponse(Employee e) {
        if (e == null) return null;
        return EmployeeResponse.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .departmentId(e.getDepartmentId())
                .employeeId(e.getEmployeeId())
                .level(e.getLevel())
                .designation(e.getDesignation())
                .joiningDate(e.getJoiningDate())
                .status(e.isOffboarded() ? "OFFBOARDED" : "ACTIVE")
                .offboardedAt(e.getOffboardedAt())
                .offboardReason(e.getOffboardReason())
                .build();
    }
}
