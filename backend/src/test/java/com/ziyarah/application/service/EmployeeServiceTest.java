package com.ziyarah.application.service;

import com.ziyarah.application.dto.request.CreateEmployeeRequest;
import com.ziyarah.application.dto.response.EmployeeResponse;
import com.ziyarah.domain.entity.Employee;
import com.ziyarah.domain.enums.EmployeeLevel;
import com.ziyarah.domain.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void createEmployee_ShouldSaveAndReturnResponse() {
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .userId(UUID.randomUUID())
                .employeeId("EMP001")
                .level(EmployeeLevel.EMPLOYEE)
                .designation("Developer")
                .build();

        Employee employee = new Employee();
        employee.setId(UUID.randomUUID());
        employee.setEmployeeId("EMP001");

        when(employeeRepository.existsByEmployeeId(anyString())).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        EmployeeResponse response = employeeService.createEmployee(request);

        assertNotNull(response);
        assertEquals("EMP001", response.getEmployeeId());
    }
}
