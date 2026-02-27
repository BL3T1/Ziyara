package com.ziyarah.application.service;

import com.ziyarah.application.dto.request.CreateEmployeeRequest;
import com.ziyarah.application.dto.request.UpdateEmployeeRequest;
import com.ziyarah.application.dto.response.EmployeeResponse;
import com.ziyarah.domain.entity.Employee;
import com.ziyarah.domain.repository.EmployeeRepository;
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
        return employeeRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    private EmployeeResponse mapToResponse(Employee employee) {
        return EmployeeResponse.builder()
                .id(employee.getId())
                .userId(employee.getUserId())
                .departmentId(employee.getDepartmentId())
                .employeeId(employee.getEmployeeId())
                .level(employee.getLevel())
                .designation(employee.getDesignation())
                .joiningDate(employee.getJoiningDate())
                .build();
    }
}
