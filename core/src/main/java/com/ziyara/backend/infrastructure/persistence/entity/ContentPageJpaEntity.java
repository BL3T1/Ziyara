package com.ziyara.backend.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "web_content_pages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentPageJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_en", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, Object> contentEn = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_ar", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, Object> contentAr = new HashMap<>();

    @Column(name = "published", nullable = false)
    @Builder.Default
    private Boolean published = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (published == null) {
            published = true;
        }
        if (contentEn == null) {
            contentEn = new HashMap<>();
        }
        if (contentAr == null) {
            contentAr = new HashMap<>();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        if (contentEn == null) {
            contentEn = new HashMap<>();
        }
        if (contentAr == null) {
            contentAr = new HashMap<>();
        }
        updatedAt = LocalDateTime.now();
    }
}
