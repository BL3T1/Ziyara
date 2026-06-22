package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateEmployeeRequest;
import com.ziyara.backend.application.dto.request.UpdateEmployeeRequest;
import com.ziyara.backend.application.dto.response.EmployeeResponse;
import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.domain.entity.Employee;
import com.ziyara.backend.domain.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
        try {
            return employeeRepository.findAll().stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("getAllEmployees failed: {}", e.getMessage());
            return List.of();
        }
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

        if (employeeRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new BusinessException("Employee ID already exists");
        }

        Employee employee = new Employee();
        employee.setUserId(request.getUserId());
        employee.setDepartmentId(request.getDepartmentId());
        employee.setEmployeeId(request.getEmployeeId());
        employee.setLevel(request.getLevel());
        employee.setDesignation(request.getDesignation());
        employee.setJoiningDate(LocalDateTime.now());

        return mapToResponse(employeeRepository.save(employee));
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
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (employee.isOffboarded()) {
            throw new BusinessException("Employee has already been offboarded");
        }

        employee.setOffboardedAt(LocalDateTime.now());
        employee.setOffboardedBy(actorUserId);
        employee.setOffboardReason(reason);
        employeeRepository.save(employee);

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
