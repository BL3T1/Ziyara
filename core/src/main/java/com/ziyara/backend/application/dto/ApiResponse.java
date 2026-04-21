package com.ziyara.backend.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO: ApiResponse
 * Generic API response wrapper
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Generic API response")
public class ApiResponse<T> {
    
    @Schema(description = "Success flag")
    private boolean success;
    
    @Schema(description = "Response message")
    private String message;
    
    @Schema(description = "Response data")
    private T data;
    
    @Schema(description = "Error details")
    private String error;

    @Schema(description = "Stable machine-readable error code (errors only)")
    private String errorCode;

    @Schema(description = "Request correlation id from logging MDC (errors only)")
    private String correlationId;
    
    @Schema(description = "Response timestamp")
    private LocalDateTime timestamp;
    
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.setSuccess(true);
        r.setData(data);
        r.setTimestamp(LocalDateTime.now());
        return r;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.setSuccess(true);
        r.setMessage(message);
        r.setData(data);
        r.setTimestamp(LocalDateTime.now());
        return r;
    }

    public static <T> ApiResponse<T> error(String error) {
        ApiResponse<T> r = new ApiResponse<>();
        r.setSuccess(false);
        r.setError(error);
        r.setTimestamp(LocalDateTime.now());
        return r;
    }

    /** Error text plus machine-readable {@code errorCode} (distinct from {@link #error(String, String)} message+detail). */
    public static <T> ApiResponse<T> errorCoded(String error, String errorCode) {
        ApiResponse<T> r = error(error);
        r.setErrorCode(errorCode);
        return r;
    }

    public static <T> ApiResponse<T> error(String message, String error) {
        ApiResponse<T> r = new ApiResponse<>();
        r.setSuccess(false);
        r.setMessage(message);
        r.setError(error);
        r.setTimestamp(LocalDateTime.now());
        return r;
    }

    public static <T> ApiResponse<T> error(String message, String error, String errorCode) {
        ApiResponse<T> r = error(message, error);
        r.setErrorCode(errorCode);
        return r;
    }
}
