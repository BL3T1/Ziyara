package com.ziyara.backend.presentation.exception;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.exception.MfaEnrollmentRequiredException;
import com.ziyara.backend.application.exception.RateLimitedException;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.application.exception.UnauthorizedException;
import com.ziyara.backend.application.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler
 * Handles all exceptions and returns appropriate HTTP responses
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    private static <T> ApiResponse<T> withCorrelation(ApiResponse<T> body) {
        String ref = MDC.get("correlationId");
        if (ref != null && !ref.isBlank()) {
            body.setCorrelationId(ref);
        }
        return body;
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("Validation error: {}", errors);
        return ResponseEntity.badRequest()
                .body(withCorrelation(ApiResponse.error("Validation failed", errors.toString(), "VALIDATION_FAILED")));
    }
    
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(withCorrelation(ApiResponse.errorCoded("Invalid email or password", "BAD_CREDENTIALS")));
    }
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(withCorrelation(ApiResponse.errorCoded(ex.getMessage(), "AUTHENTICATION_FAILED")));
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(withCorrelation(ApiResponse.errorCoded("Access denied", "ACCESS_DENIED")));
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(withCorrelation(ApiResponse.errorCoded(ex.getMessage(), "NOT_FOUND")));
    }
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Business error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(withCorrelation(ApiResponse.errorCoded(ex.getMessage(), "BUSINESS_RULE")));
    }

    @ExceptionHandler(RateLimitedException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimited(RateLimitedException ex) {
        log.warn("Rate limited: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", "60")
                .body(withCorrelation(ApiResponse.errorCoded(ex.getMessage(), "RATE_LIMITED")));
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(UnauthorizedException ex) {
        log.warn("Unauthorized: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(withCorrelation(ApiResponse.errorCoded(ex.getMessage(), "FORBIDDEN")));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(withCorrelation(ApiResponse.errorCoded(ex.getMessage(), "BAD_REQUEST")));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter: {}", ex.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(withCorrelation(ApiResponse.errorCoded(
                        "Required parameter '" + ex.getParameterName() + "' is missing",
                        "MISSING_PARAMETER")));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String name = ex.getName();
        String value = ex.getValue() != null ? ex.getValue().toString() : "null";
        log.warn("Type mismatch: parameter '{}' value '{}'", name, value);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(withCorrelation(ApiResponse.errorCoded(
                        "Invalid value '" + value + "' for parameter '" + name + "'",
                        "INVALID_PARAMETER")));
    }

    @ExceptionHandler(AuthService.AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthService.AuthenticationException ex) {
        log.warn("Auth error: {}", ex.getMessage());
        // Distinguish "MFA code required" from generic auth failures so the mobile client
        // can show the TOTP challenge screen instead of a generic error message.
        String code = "MFA code required".equals(ex.getMessage())
                ? "MFA_CODE_REQUIRED"
                : "AUTH_FAILED";
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(withCorrelation(ApiResponse.errorCoded(ex.getMessage(), code)));
    }

    @ExceptionHandler(MfaEnrollmentRequiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleMfaEnrollmentRequired(MfaEnrollmentRequiredException ex) {
        log.warn("MFA enrollment required: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(withCorrelation(ApiResponse.errorCoded(ex.getMessage(), "MFA_ENROLLMENT_REQUIRED")));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUpload(MaxUploadSizeExceededException ex) {
        log.warn("Upload too large: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(withCorrelation(ApiResponse.errorCoded("File too large (max 10MB per file)", "PAYLOAD_TOO_LARGE")));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(withCorrelation(ApiResponse.errorCoded(
                        "Data constraint violation. Check required fields and unique values.",
                        "DATA_INTEGRITY")));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error: ", ex);
        String ref = MDC.get("correlationId");
        boolean verboseError = activeProfile != null
                && (activeProfile.contains("dev") || activeProfile.contains("docker"));
        String message = verboseError
                ? (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName())
                : (ref != null && !ref.isBlank()
                        ? "An unexpected error occurred (reference: " + ref + ")"
                        : "An unexpected error occurred");
        ApiResponse<Void> body = ApiResponse.errorCoded(message, "INTERNAL_ERROR");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(withCorrelation(body));
    }
}
