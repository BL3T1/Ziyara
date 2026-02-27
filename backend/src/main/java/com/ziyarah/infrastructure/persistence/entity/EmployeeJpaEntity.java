package com.ziyarah.infrastructure.persistence.entity;

import com.ziyarah.domain.enums.EmployeeLevel;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity: EmployeeJpaEntity
 * Maps to 'employees' table
 */
@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "department_id")
    private UUID departmentId;
    
    @Column(name = "employee_id", unique = true, length = 50)
    private String employeeId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "level")
    private EmployeeLevel level;
    
    @Column(name = "designation")
    private String designation;
    
    @Column(name = "joining_date")
    private LocalDateTime joiningDate;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
