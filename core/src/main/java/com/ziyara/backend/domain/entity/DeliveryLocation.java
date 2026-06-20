package com.ziyara.backend.domain.entity;

import java.time.Instant;
import java.util.UUID;

public class DeliveryLocation {
    private UUID id;
    private UUID bookingId;
    private double latitude;
    private double longitude;
    private String status;
    private Instant recordedAt;

    public DeliveryLocation() {}

    public DeliveryLocation(UUID id, UUID bookingId, double latitude, double longitude,
                             String status, Instant recordedAt) {
        this.id = id;
        this.bookingId = bookingId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
        this.recordedAt = recordedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
}
