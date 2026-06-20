package com.ziyara.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sys_pii_field_registry", uniqueConstraints = @UniqueConstraint(columnNames = {"table_name", "column_name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PiiFieldRegistryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "table_name", nullable = false, length = 100)
    private String tableName;

    @Column(name = "column_name", nullable = false, length = 100)
    private String columnName;

    @Column(name = "pii_category", nullable = false, length = 50)
    private String piiCategory;

    @Column(name = "encryption_required", nullable = false)
    private Boolean encryptionRequired;

    @Column(name = "gdpr_article", length = 100)
    private String gdprArticle;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;
}
