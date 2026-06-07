package com.ziyara.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerJpaEntity {

    @Id
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "id_document_url")
    private String idDocumentUrl;

    @Column(name = "id_document_type", length = 50)
    private String idDocumentType;

    @Column(name = "id_document_number", length = 100)
    private String idDocumentNumber;

    @Column(name = "id_document_number_cipher", columnDefinition = "TEXT")
    private String idDocumentNumberCipher;

    @Column(name = "preferred_currency", length = 3)
    private String preferredCurrency;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "nationality", length = 100)
    private String nationality;

    @Column(name = "pii_encryption_version")
    private Short piiEncryptionVersion;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (preferredCurrency == null) preferredCurrency = "USD";
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
