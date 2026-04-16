package com.ziyara.backend.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update a booking (only when modifiable)")
public class UpdateBookingRequest {

    @Schema(description = "Check-in date")
    private LocalDate checkInDate;

    @Schema(description = "Check-out date")
    private LocalDate checkOutDate;

    @Schema(description = "Number of guests")
    private Integer guests;

    @Schema(description = "Number of rooms")
    private Integer rooms;

    @Schema(description = "Special requests")
    private String specialRequests;
}
