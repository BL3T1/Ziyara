package com.ziyara.backend.application.dto.response;

import com.ziyara.backend.domain.enums.DiscountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Discount code response")
public class DiscountResponse {

    private UUID id;
    private String code;
    private String description;
    private String type;
    private BigDecimal value;
    private BigDecimal minBookingAmount;
    private BigDecimal maxDiscountAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer usageLimit;
    private Integer usageCount;
    private DiscountStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** COMPANY, PROVIDER, or BOTH */
    private String sponsor;

    private UUID providerId;
    private List<UUID> applicableServiceIds;
    private List<UUID> applicableMenuSectionIds;
    private List<UUID> applicableMenuItemIds;
    private List<UUID> applicableRoomTypeIds;
}
