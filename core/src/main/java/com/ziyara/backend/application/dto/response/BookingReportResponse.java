package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Report: booking counts and by day in date range")
public class BookingReportResponse {

    @Schema(description = "Start date")
    private LocalDate start;

    @Schema(description = "End date")
    private LocalDate end;

    @Schema(description = "Total bookings in range")
    private long totalBookings;

    @Schema(description = "Bookings per day")
    private List<DayCount> byDay;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DayCount {
        private LocalDate date;
        private long count;
    }
}
