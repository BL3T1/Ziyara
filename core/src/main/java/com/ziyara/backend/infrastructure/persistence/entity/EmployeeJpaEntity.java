package com.ziyara.backend.infrastructure.persistence.entity;

import com.ziyara.backend.domain.enums.EmployeeLevel;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity: EmployeeJpaEntity
 * Maps to 'employees' table
 */
@Entity
@Table(name = "sys_employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeJpaEntity {
    
    @Id
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;
    
    @Column(name = "department_id")
    private UUID departmentId;
    
    @Column(name = "employee_code", nullable = false, unique = true, length = 20)
    private String employeeCode;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private EmployeeLevel level;
    
    @Column(name = "job_title", length = 100)
    private String jobTitle;
    
    @Column(name = "hire_date", nullable = false)
    private java.time.LocalDate hireDate;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "offboarded_at")
    private LocalDateTime offboardedAt;

    @Column(name = "offboarded_by")
    private UUID offboardedBy;

    @Column(name = "offboard_reason", length = 500)
    private String offboardReason;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
