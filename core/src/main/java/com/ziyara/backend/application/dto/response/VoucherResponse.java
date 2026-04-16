package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Booking voucher (reference, dates, service, customer, amount)")
public class VoucherResponse {

    private UUID bookingId;
    private String bookingReference;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String serviceName;
    private UUID serviceId;
    private String customerEmail;
    private UUID customerId;
    private BigDecimal totalAmount;
    private String currency;
}
