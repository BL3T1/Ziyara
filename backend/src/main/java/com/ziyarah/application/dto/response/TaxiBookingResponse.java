package com.ziyarah.application.dto.response;

import com.ziyarah.domain.enums.TaxiStatus;
import com.ziyarah.domain.enums.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Taxi booking details")
public class TaxiBookingResponse {
    
    @Schema(description = "Internal ID")
    private UUID id;
    
    @Schema(description = "Core booking ID")
    private UUID bookingId;
    
    @Schema(description = "Driver ID")
    private UUID driverId;
    
    @Schema(description = "Vehicle type")
    private VehicleType vehicleType;
    
    @Schema(description = "Pickup location")
    private String pickupLocation;
    
    @Schema(description = "Destination location")
    private String destinationLocation;
    
    @Schema(description = "Scheduled timestamp")
    private LocalDateTime scheduledAt;
    
    @Schema(description = "Started timestamp")
    private LocalDateTime startedAt;
    
    @Schema(description = "Completed timestamp")
    private LocalDateTime completedAt;
    
    @Schema(description = "Estimated distance")
    private BigDecimal estimatedDistance;
    
    @Schema(description = "Estimated price")
    private BigDecimal estimatedPrice;
    
    @Schema(description = "Booking status")
    private TaxiStatus status;
    
    @Schema(description = "License plate")
    private String licensePlate;
    
    @Schema(description = "Driver name")
    private String driverName;
    
    @Schema(description = "Vehicle model")
    private String vehicleModel;
}
