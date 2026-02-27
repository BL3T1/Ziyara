package com.ziyarah.application.service;

import com.ziyarah.application.dto.response.DepartmentResponse;
import com.ziyarah.domain.entity.Department;
import com.ziyarah.domain.repository.DepartmentRepository;
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
    
    private DepartmentResponse mapToResponse(Department dept) {
        return DepartmentResponse.builder()
                .id(dept.getId())
                .name(dept.getName())
                .description(dept.getDescription())
                .managerId(dept.getManagerId())
                .build();
    }
}
