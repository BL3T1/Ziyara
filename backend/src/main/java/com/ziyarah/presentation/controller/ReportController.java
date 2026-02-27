package com.ziyarah.presentation.controller;

import com.ziyarah.application.dto.ApiResponse;
import com.ziyarah.application.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<Void>> generateRevenueReport(@RequestParam LocalDate start, @RequestParam LocalDate end) {
        reportService.generateRevenueReport(start, end);
        return ResponseEntity.ok(ApiResponse.success(null, "Revenue report generation initiated"));
    }

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<Void>> generateBookingReport(@RequestParam LocalDate start, @RequestParam LocalDate end) {
        reportService.generateBookingReport(start, end);
        return ResponseEntity.ok(ApiResponse.success(null, "Booking report generation initiated"));
    }
}
