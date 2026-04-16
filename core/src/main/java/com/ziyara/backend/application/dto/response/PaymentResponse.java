package com.ziyara.backend.application.dto.response;

import com.ziyara.backend.domain.enums.PaymentMethod;
import com.ziyara.backend.domain.enums.PaymentStatus;
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
@Schema(description = "Payment details")
public class PaymentResponse {
    
    @Schema(description = "Payment ID")
    private UUID id;
    
    @Schema(description = "Booking ID")
    private UUID bookingId;
    
    @Schema(description = "Payment amount")
    private BigDecimal amount;
    
    @Schema(description = "Currency")
    private String currency;
    
    @Schema(description = "Payment method")
    private PaymentMethod method;
    
    @Schema(description = "Payment status")
    private PaymentStatus status;
    
    @Schema(description = "Transaction reference")
    private String transactionReference;
    
    @Schema(description = "External gateway transaction ID")
    private String gatewayReference;
    
    @Schema(description = "3DS status (e.g. AUTHENTICATED, NOT_REQUIRED, FAILED)")
    private String threeDsStatus;
    
    @Schema(description = "Payment gateway name")
    private String gatewayName;
    
    @Schema(description = "3DS redirect URL (when gateway requires authentication)")
    private String redirectUrl;
    
    @Schema(description = "Error message (if failed)")
    private String errorMessage;
    
    @Schema(description = "Processed timestamp")
    private LocalDateTime processedAt;
    
    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    public static PaymentResponseBuilder builder() {
        return new PaymentResponseBuilder();
    }

    public static class PaymentResponseBuilder {
        private UUID id;
        private UUID bookingId;
        private BigDecimal amount;
        private String currency;
        private PaymentMethod method;
        private PaymentStatus status;
        private String transactionReference;
        private String gatewayReference;
        private String threeDsStatus;
        private String gatewayName;
        private String redirectUrl;
        private String errorMessage;
        private LocalDateTime processedAt;
        private LocalDateTime createdAt;

        public PaymentResponseBuilder id(UUID id) { this.id = id; return this; }
        public PaymentResponseBuilder bookingId(UUID bookingId) { this.bookingId = bookingId; return this; }
        public PaymentResponseBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public PaymentResponseBuilder currency(String currency) { this.currency = currency; return this; }
        public PaymentResponseBuilder method(PaymentMethod method) { this.method = method; return this; }
        public PaymentResponseBuilder status(PaymentStatus status) { this.status = status; return this; }
        public PaymentResponseBuilder transactionReference(String ref) { this.transactionReference = ref; return this; }
        public PaymentResponseBuilder gatewayReference(String ref) { this.gatewayReference = ref; return this; }
        public PaymentResponseBuilder threeDsStatus(String s) { this.threeDsStatus = s; return this; }
        public PaymentResponseBuilder gatewayName(String name) { this.gatewayName = name; return this; }
        public PaymentResponseBuilder redirectUrl(String url) { this.redirectUrl = url; return this; }
        public PaymentResponseBuilder errorMessage(String msg) { this.errorMessage = msg; return this; }
        public PaymentResponseBuilder processedAt(LocalDateTime at) { this.processedAt = at; return this; }
        public PaymentResponseBuilder createdAt(LocalDateTime at) { this.createdAt = at; return this; }
        public PaymentResponse build() {
            PaymentResponse r = new PaymentResponse();
            r.setId(id); r.setBookingId(bookingId); r.setAmount(amount); r.setCurrency(currency);
            r.setMethod(method); r.setStatus(status); r.setTransactionReference(transactionReference);
            r.setGatewayReference(gatewayReference); r.setThreeDsStatus(threeDsStatus);
            r.setGatewayName(gatewayName); r.setRedirectUrl(redirectUrl); r.setErrorMessage(errorMessage);
            r.setProcessedAt(processedAt); r.setCreatedAt(createdAt);
            return r;
        }
    }
}
