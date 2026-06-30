package com.ziyara.backend.domain.usecase.employee;

import com.ziyara.backend.domain.entity.Employee;
import com.ziyara.backend.domain.repository.EmployeeRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public class OffboardEmployeeUseCase {

    private final EmployeeRepository employeeRepository;

    public OffboardEmployeeUseCase(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public Result execute(Input input) {
        Employee employee = employeeRepository.findById(input.employeeId())
                .orElse(null);
        if (employee == null) {
            return Result.failure("Employee not found");
        }
        if (employee.isOffboarded()) {
            return Result.failure("Employee has already been offboarded");
        }
        employee.setOffboardedAt(LocalDateTime.now());
        employee.setOffboardedBy(input.actorId());
        employee.setOffboardReason(input.reason());
        employeeRepository.save(employee);
        return Result.success();
    }

    public record Input(UUID employeeId, UUID actorId, String reason) {}

    public record Result(boolean success, String error) {
        public static Result success() { return new Result(true, null); }
        public static Result failure(String error) { return new Result(false, error); }
    }
}
