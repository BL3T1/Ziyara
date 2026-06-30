package com.ziyara.backend.domain.usecase.employee;

import com.ziyara.backend.domain.entity.Employee;
import com.ziyara.backend.domain.enums.EmployeeLevel;
import com.ziyara.backend.domain.repository.EmployeeRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public class OnboardEmployeeUseCase {

    private final EmployeeRepository employeeRepository;

    public OnboardEmployeeUseCase(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public Result execute(Input input) {
        if (employeeRepository.existsByEmployeeId(input.employeeId())) {
            return Result.failure("Employee ID already exists");
        }
        Employee employee = new Employee();
        employee.setUserId(input.userId());
        employee.setDepartmentId(input.departmentId());
        employee.setEmployeeId(input.employeeId());
        employee.setLevel(input.level());
        employee.setDesignation(input.designation());
        employee.setJoiningDate(LocalDateTime.now());
        return Result.success(employeeRepository.save(employee));
    }

    public record Input(
            UUID userId,
            UUID departmentId,
            String employeeId,
            EmployeeLevel level,
            String designation
    ) {}

    public record Result(boolean success, Employee employee, String error) {
        public static Result success(Employee employee) { return new Result(true, employee, null); }
        public static Result failure(String error) { return new Result(false, null, error); }
    }
}
