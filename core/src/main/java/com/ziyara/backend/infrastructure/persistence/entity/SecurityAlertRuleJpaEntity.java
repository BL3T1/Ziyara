package com.ziyara.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sys_security_alert_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityAlertRuleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false)
    private Integer threshold;

    @Column(name = "time_window_minutes", nullable = false)
    private Integer timeWindowMinutes;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(name = "cooldown_minutes", nullable = false)
    private Integer cooldownMinutes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (enabled == null) {
            enabled = Boolean.TRUE;
        }
        if (cooldownMinutes == null) {
            cooldownMinutes = 60;
        }
    }
}
