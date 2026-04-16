package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.DepartmentResponse;
import com.ziyara.backend.application.locale.RequestLocaleHolder;
import com.ziyara.backend.domain.entity.Department;
import com.ziyara.backend.domain.repository.DepartmentRepository;
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
 * Service: DepartmentService
 * Handles organizational structure management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentService {
    
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    
    @Transactional
    public DepartmentResponse createDepartment(String name, String description, UUID managerId) {
        log.info("Creating department: {}", name);
        
        if (departmentRepository.findByName(name).isPresent()) {
            throw new RuntimeException("Department already exists");
        }
        
        Department dept = new Department();
        dept.setName(name);
        dept.setDescription(description);
        dept.setManagerId(managerId);
        
        return mapToResponse(departmentRepository.save(dept));
    }
    
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartment(UUID id) {
        return departmentRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
    }

    @Transactional
    public DepartmentResponse updateDepartment(UUID id, String name, String description, UUID managerId) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        if (name != null && !name.isBlank()) dept.setName(name);
        if (description != null) dept.setDescription(description);
        if (managerId != null) dept.setManagerId(managerId);
        return mapToResponse(departmentRepository.save(dept));
    }

    @Transactional
    public void deleteDepartment(UUID id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        if (!employeeRepository.findByDepartmentId(id).isEmpty()) {
            throw new IllegalStateException("Cannot delete department: it has employees assigned. Reassign or offboard them first.");
        }
        departmentRepository.deleteById(id);
        log.info("Department deleted: {}", id);
    }
    
    private DepartmentResponse mapToResponse(Department dept) {
        return DepartmentResponse.builder()
                .id(dept.getId())
                .name(RequestLocaleHolder.localized(dept.getName(), dept.getNameAr()))
                .description(RequestLocaleHolder.localized(dept.getDescription(), dept.getDescriptionAr()))
                .managerId(dept.getManagerId())
                .build();
    }
}
