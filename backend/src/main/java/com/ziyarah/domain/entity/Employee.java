package com.ziyarah.domain.entity;

import com.ziyarah.domain.enums.EmployeeLevel;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain Entity: Employee
 * Represents staff members within the system
 */
public class Employee {
    
    private UUID id;
    private UUID userId; // Reference to the core User entity
    private UUID departmentId;
    private String employeeId; // Internal employee numbering
    private EmployeeLevel level;
    private String designation;
    private LocalDateTime joiningDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Domain behavior methods
    public boolean isActive() {
        return joiningDate != null && joiningDate.isBefore(LocalDateTime.now());
    }

    // Constructors
    public Employee() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Employee(UUID id, UUID userId, EmployeeLevel level, String designation) {
        this();
        this.id = id;
        this.userId = userId;
        this.level = level;
        this.designation = designation;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public EmployeeLevel getLevel() { return level; }
    public void setLevel(EmployeeLevel level) { this.level = level; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public LocalDateTime getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDateTime joiningDate) { this.joiningDate = joiningDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
