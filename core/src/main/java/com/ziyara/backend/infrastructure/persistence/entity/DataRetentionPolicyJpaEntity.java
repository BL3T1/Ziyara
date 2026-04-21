package com.ziyara.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sys_data_retention_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataRetentionPolicyJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "entity_type", nullable = false, unique = true, length = 100)
    private String entityType;

    @Column(name = "retention_period_days", nullable = false)
    private Integer retentionPeriodDays;

    @Column(name = "retention_condition", length = 255)
    private String retentionCondition;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "last_execution")
    private LocalDateTime lastExecution;

    @Column(name = "next_execution")
    private LocalDateTime nextExecution;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
