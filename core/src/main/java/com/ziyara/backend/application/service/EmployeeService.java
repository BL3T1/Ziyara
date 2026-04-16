package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateEmployeeRequest;
import com.ziyara.backend.application.dto.request.UpdateEmployeeRequest;
import com.ziyara.backend.application.dto.response.EmployeeResponse;
import com.ziyara.backend.domain.entity.Employee;
import com.ziyara.backend.domain.repository.EmployeeRepository;
import com.ziyara.backend.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service: EmployeeService
 * Handles internal employee management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeService {
    
    private final EmployeeRepository employeeRepository;
    
    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        log.info("Creating employee for user: {}", request.getUserId());
        
        if (employeeRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new RuntimeException("Employee ID already exists");
        }
        
        Employee employee = new Employee();
        employee.setUserId(request.getUserId());
        employee.setDepartmentId(request.getDepartmentId());
        employee.setEmployeeId(request.getEmployeeId());
        employee.setLevel(request.getLevel());
        employee.setDesignation(request.getDesignation());
        employee.setJoiningDate(java.time.LocalDateTime.now());
        
        return mapToResponse(employeeRepository.save(employee));
    }
    
    @Transactional
    public EmployeeResponse updateEmployee(UUID id, UpdateEmployeeRequest request) {
        log.info("Updating employee: {}", id);
        
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        
        if (request.getDepartmentId() != null) employee.setDepartmentId(request.getDepartmentId());
        if (request.getLevel() != null) employee.setLevel(request.getLevel());
        if (request.getDesignation() != null) employee.setDesignation(request.getDesignation());
        
        return mapToResponse(employeeRepository.save(employee));
    }
    
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployee(UUID id) {
        return employeeRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }
    
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

    /** Offboard (delete) employee. */
    @Transactional
    public void offboardEmployee(UUID id) {
        if (!employeeRepository.findById(id).isPresent()) {
            throw new ResourceNotFoundException("Employee not found");
        }
        employeeRepository.deleteById(id);
        log.info("Employee offboarded (deleted): {}", id);
    }
    
    private EmployeeResponse mapToResponse(Employee employee) {
        return EmployeeResponse.builder()
                .id(employee != null ? employee.getId() : null)
                .userId(employee != null ? employee.getUserId() : null)
                .departmentId(employee != null ? employee.getDepartmentId() : null)
                .employeeId(employee != null ? employee.getEmployeeId() : null)
                .level(employee != null ? employee.getLevel() : null)
                .designation(employee != null ? employee.getDesignation() : null)
                .joiningDate(employee != null ? employee.getJoiningDate() : null)
                .build();
    }
}
