package com.ziyara.backend.infrastructure.persistence.entity;

import com.ziyara.backend.domain.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * JPA Entity: BookingJpaEntity
 * Infrastructure layer representation of Booking
 * Maps to 'bookings' table in PostgreSQL
 */
@Entity
@Table(name = "bkg_bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "booking_reference", nullable = false, unique = true, length = 20)
    private String bookingReference;
    
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
    
    @Column(name = "service_id", nullable = false)
    private UUID serviceId;
    
    @Column(name = "discount_code_id")
    private UUID discountCodeId;
    
    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;
    
    @Column(name = "check_out_date")
    private LocalDate checkOutDate;
    
    @Column(name = "guests", nullable = false)
    private Integer guests = 1;
    
    @Column(name = "rooms")
    private Integer rooms = 1;
    
    @Column(name = "base_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseAmount;
    
    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    @Column(name = "tax_amount", precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    
    @Column(name = "commission_amount", precision = 12, scale = 2)
    private BigDecimal commissionAmount = BigDecimal.ZERO;
    
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "currency", length = 3)
    private String currency = "USD";
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status;
    
    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;
    
    @Column(name = "id_document_url", length = 500)
    private String idDocumentUrl;
    
    @Column(name = "id_document_verified")
    private Boolean idDocumentVerified = false;
    
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "delay_reason", columnDefinition = "TEXT")
    private String delayReason;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejected_by")
    private UUID rejectedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "discount_context_menu_item_ids", columnDefinition = "jsonb")
    private List<String> discountContextMenuItemIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "discount_context_menu_section_ids", columnDefinition = "jsonb")
    private List<String> discountContextMenuSectionIds;

    @Column(name = "discount_context_room_type_id")
    private UUID discountContextRoomTypeId;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = BookingStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
