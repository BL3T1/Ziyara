package com.ziyarah.presentation.controller;

import com.ziyarah.application.dto.ApiResponse;
import com.ziyarah.application.dto.BookingRequest;
import com.ziyarah.application.dto.BookingResponse;
import com.ziyarah.application.service.BookingService;
import com.ziyarah.domain.entity.Booking;
import com.ziyarah.domain.repository.BookingRepository;
import com.ziyarah.domain.repository.ServiceRepository;
import com.ziyarah.infrastructure.security.JwtService;
import com.ziyarah.presentation.exception.BusinessException;
import com.ziyarah.presentation.exception.ResourceNotFoundException;
import com.ziyarah.presentation.exception.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller: BookingController
 * Handles booking endpoints
 */
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking management APIs")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {
    
    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;
    private final JwtService jwtService;
    
    @GetMapping
    @Operation(summary = "Get all bookings", description = "Retrieve all bookings for the authenticated user")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getAllBookings(
            @RequestHeader("Authorization") String authHeader
    ) {
        UUID userId = extractUserId(authHeader);
        List<Booking> bookings = bookingRepository.findByCustomerId(userId);
        List<BookingResponse> responses = bookings.stream()
                .map(this::toBookingResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get booking by ID", description = "Retrieve a specific booking by ID")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authHeader
    ) {
        UUID userId = extractUserId(authHeader);
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        
        if (!booking.getCustomerId().equals(userId)) {
            throw new UnauthorizedException("You don't have access to this booking");
        }
        
        return ResponseEntity.ok(ApiResponse.success(toBookingResponse(booking)));
    }
    
    @GetMapping("/reference/{reference}")
    @Operation(summary = "Get booking by reference", description = "Retrieve a booking by its reference number")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByReference(
            @PathVariable String reference,
            @RequestHeader("Authorization") String authHeader
    ) {
        UUID userId = extractUserId(authHeader);
        Booking booking = bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        
        if (!booking.getCustomerId().equals(userId)) {
            throw new UnauthorizedException("You don't have access to this booking");
        }
        
        return ResponseEntity.ok(ApiResponse.success(toBookingResponse(booking)));
    }
    
    @PostMapping
    @Operation(summary = "Create booking", description = "Create a new booking")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody BookingRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        UUID userId = extractUserId(authHeader);
        
        // Validate service exists
        com.ziyarah.domain.entity.Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        
        // Check availability
        if (request.getCheckOutDate() != null) {
            boolean hasConflict = bookingRepository.hasConflictingBooking(
                    request.getServiceId(),
                    request.getCheckInDate(),
                    request.getCheckOutDate()
            );
            if (hasConflict) {
                throw new BusinessException("Service is not available for the selected dates");
            }
        }
        
        // Create booking
        Booking booking = new Booking();
        booking.setCustomerId(userId);
        booking.setServiceId(request.getServiceId());
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setGuests(request.getGuests());
        booking.setRooms(request.getRooms());
        booking.setSpecialRequests(request.getSpecialRequests());
        booking.setIdDocumentUrl(request.getIdDocumentUrl());
        booking.setCurrency(request.getCurrency());
        booking.setBaseAmount(service.getBasePrice());
        booking.setTotalAmount(service.getBasePrice());
        
        // Generate booking reference
        booking.setBookingReference(generateBookingReference());
        
        Booking savedBooking = bookingRepository.save(booking);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", toBookingResponse(savedBooking)));
    }
    
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel booking", description = "Cancel an existing booking")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            @RequestHeader("Authorization") String authHeader
    ) {
        UUID userId = extractUserId(authHeader);
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        
        if (!booking.getCustomerId().equals(userId)) {
            throw new UnauthorizedException("You don't have access to this booking");
        }
        
        if (!booking.canBeCancelled()) {
            throw new BusinessException("Booking cannot be cancelled in its current status");
        }
        
        booking.cancel(userId, reason);
        Booking savedBooking = bookingRepository.save(booking);
        
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", toBookingResponse(savedBooking)));
    }
    
    private UUID extractUserId(String authHeader) {
        String token = authHeader.substring(7);
        return UUID.fromString(jwtService.extractUserId(token));
    }
    
    private String generateBookingReference() {
        return "ZYB" + System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 10000));
    }
    
    private BookingResponse toBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .customerId(booking.getCustomerId())
                .serviceId(booking.getServiceId())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .guests(booking.getGuests())
                .rooms(booking.getRooms())
                .baseAmount(booking.getBaseAmount())
                .discountAmount(booking.getDiscountAmount())
                .taxAmount(booking.getTaxAmount())
                .totalAmount(booking.getTotalAmount())
                .currency(booking.getCurrency())
                .status(booking.getStatus())
                .specialRequests(booking.getSpecialRequests())
                .idDocumentVerified(booking.isIdDocumentVerified())
                .confirmedAt(booking.getConfirmedAt())
                .cancelledAt(booking.getCancelledAt())
                .cancellationReason(booking.getCancellationReason())
                .createdAt(booking.getCreatedAt())
                .canBeCancelled(booking.canBeCancelled())
                .canBeModified(booking.canBeModified())
                .build();
    }
    
}
