package com.ziyara.backend.infrastructure.payment;

import com.ziyara.backend.application.dto.payment.GatewayPaymentResponse;
import com.ziyara.backend.application.dto.payment.GatewayRefundResult;
import com.ziyara.backend.application.dto.payment.TokenizedPaymentCommand;
import com.ziyara.backend.domain.enums.PaymentStatus;
import com.ziyara.backend.domain.payment.PaymentProvider;
import com.ziyara.backend.infrastructure.config.PaymentGatewayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

/**
 * Stripe payment gateway implementation. Activated when app.payment.gateway.provider=stripe.
 * Uses Stripe's PaymentIntents API for 3DS-capable card payments.
 * PCI note: raw card data is never handled here — only tokens from Stripe.js/Elements.
 */
@Component
@ConditionalOnProperty(name = "app.payment.gateway.provider", havingValue = "stripe")
@RequiredArgsConstructor
@Slf4j
public class StripePaymentProvider implements PaymentProvider {

    private static final String STRIPE_API = "https://api.stripe.com/v1";
    private static final String PROVIDER_NAME = "STRIPE";

    private final PaymentGatewayProperties gatewayProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public GatewayPaymentResponse initiatePayment(TokenizedPaymentCommand command) {
        log.info("Stripe: initiating payment {} amount {} {}", command.getPaymentId(), command.getAmount(), command.getCurrency());
        try {
            HttpHeaders headers = buildHeaders();
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            // Stripe amounts are in smallest currency unit (cents)
            body.add("amount", toStripeAmount(command.getAmount(), command.getCurrency()));
            body.add("currency", command.getCurrency().toLowerCase());
            body.add("payment_method", command.getPaymentToken());
            body.add("confirm", "true");
            body.add("automatic_payment_methods[enabled]", "true");
            body.add("automatic_payment_methods[allow_redirects]", "never");
            if (command.getIdempotencyKey() != null && !command.getIdempotencyKey().isBlank()) {
                headers.set("Idempotency-Key", command.getIdempotencyKey());
            }
            body.add("metadata[payment_id]", command.getPaymentId().toString());
            body.add("metadata[booking_id]", command.getBookingId().toString());

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    STRIPE_API + "/payment_intents", request, Map.class);

            return parsePaymentIntentResponse(command, response.getBody());
        } catch (HttpClientErrorException e) {
            log.error("Stripe API error ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            return GatewayPaymentResponse.builder()
                    .success(false)
                    .paymentId(command.getPaymentId())
                    .status(PaymentStatus.FAILED)
                    .errorMessage("Stripe error: " + extractStripeError(e.getResponseBodyAsString()))
                    .build();
        } catch (Exception e) {
            log.error("Stripe payment initiation failed", e);
            return GatewayPaymentResponse.builder()
                    .success(false)
                    .paymentId(command.getPaymentId())
                    .status(PaymentStatus.FAILED)
                    .errorMessage("Payment processing unavailable")
                    .build();
        }
    }

    @Override
    public GatewayRefundResult refund(String gatewayReference, BigDecimal amount, String currency, String reason) {
        log.info("Stripe: refunding {} {} for payment_intent {}", amount, currency, gatewayReference);
        try {
            HttpHeaders headers = buildHeaders();
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("payment_intent", gatewayReference);
            body.add("amount", toStripeAmount(amount, currency));
            if (reason != null && !reason.isBlank()) {
                // Stripe accepts: duplicate, fraudulent, requested_by_customer
                body.add("reason", "requested_by_customer");
                body.add("metadata[reason]", reason.substring(0, Math.min(reason.length(), 500)));
            }
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(STRIPE_API + "/refunds", request, Map.class);
            Map<?, ?> resp = response.getBody();
            String refundId = resp != null ? String.valueOf(resp.get("id")) : null;
            return GatewayRefundResult.builder()
                    .success(true)
                    .gatewayRefundId(refundId)
                    .build();
        } catch (HttpClientErrorException e) {
            log.error("Stripe refund error ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            return GatewayRefundResult.builder()
                    .success(false)
                    .errorMessage("Stripe refund error: " + extractStripeError(e.getResponseBodyAsString()))
                    .build();
        } catch (Exception e) {
            log.error("Stripe refund failed", e);
            return GatewayRefundResult.builder().success(false).errorMessage("Refund unavailable").build();
        }
    }

    @Override
    public Optional<PaymentStatus> getStatus(String gatewayReference) {
        try {
            HttpHeaders headers = buildHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    STRIPE_API + "/payment_intents/" + gatewayReference, Map.class);
            Map<?, ?> body = response.getBody();
            if (body == null) return Optional.empty();
            String status = String.valueOf(body.get("status"));
            return Optional.of(mapStripeStatus(status));
        } catch (Exception e) {
            log.warn("Stripe status check failed for {}: {}", gatewayReference, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBearerAuth(gatewayProperties.getApiKey());
        return headers;
    }

    private static String toStripeAmount(BigDecimal amount, String currency) {
        // Zero-decimal currencies (JPY, etc.) don't need multiplication
        boolean zeroDecimal = isZeroDecimalCurrency(currency);
        BigDecimal stripe = zeroDecimal
                ? amount.setScale(0, RoundingMode.HALF_UP)
                : amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP);
        return stripe.toPlainString();
    }

    private static boolean isZeroDecimalCurrency(String currency) {
        if (currency == null) return false;
        return switch (currency.toUpperCase()) {
            case "BIF", "CLP", "DJF", "GNF", "JPY", "KMF", "KRW",
                 "MGA", "PYG", "RWF", "UGX", "VND", "VUV", "XAF", "XOF", "XPF" -> true;
            default -> false;
        };
    }

    @SuppressWarnings("unchecked")
    private GatewayPaymentResponse parsePaymentIntentResponse(TokenizedPaymentCommand command, Map<?, ?> body) {
        if (body == null) {
            return GatewayPaymentResponse.builder()
                    .success(false).paymentId(command.getPaymentId())
                    .status(PaymentStatus.FAILED).errorMessage("Empty response from Stripe").build();
        }
        String status = String.valueOf(body.get("status"));
        String intentId = String.valueOf(body.get("id"));
        String redirectUrl = null;
        String threeDsStatus = "NOT_REQUIRED";

        if ("requires_action".equals(status)) {
            // 3DS redirect required
            Map<?, ?> nextAction = (Map<?, ?>) body.get("next_action");
            if (nextAction != null) {
                Map<?, ?> redirect = (Map<?, ?>) nextAction.get("redirect_to_url");
                if (redirect != null) {
                    redirectUrl = String.valueOf(redirect.get("url"));
                }
            }
            threeDsStatus = "REQUIRED";
            return GatewayPaymentResponse.builder()
                    .success(true)
                    .paymentId(command.getPaymentId())
                    .transactionReference(intentId)
                    .gatewayReference(intentId)
                    .status(PaymentStatus.PENDING)
                    .threeDsStatus(threeDsStatus)
                    .redirectUrl(redirectUrl)
                    .build();
        }

        PaymentStatus mappedStatus = mapStripeStatus(status);
        return GatewayPaymentResponse.builder()
                .success(mappedStatus == PaymentStatus.COMPLETED)
                .paymentId(command.getPaymentId())
                .transactionReference(intentId)
                .gatewayReference(intentId)
                .status(mappedStatus)
                .threeDsStatus(threeDsStatus)
                .errorMessage(mappedStatus == PaymentStatus.FAILED ? "Payment declined by Stripe" : null)
                .build();
    }

    private static PaymentStatus mapStripeStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "succeeded" -> PaymentStatus.COMPLETED;
            case "requires_payment_method", "canceled" -> PaymentStatus.FAILED;
            case "processing", "requires_action", "requires_capture", "requires_confirmation" -> PaymentStatus.PENDING;
            default -> PaymentStatus.PENDING;
        };
    }

    private static String extractStripeError(String responseBody) {
        if (responseBody == null) return "Unknown error";
        int msgIdx = responseBody.indexOf("\"message\":");
        if (msgIdx < 0) return responseBody.substring(0, Math.min(200, responseBody.length()));
        int start = responseBody.indexOf('"', msgIdx + 10) + 1;
        int end = responseBody.indexOf('"', start);
        return start > 0 && end > start ? responseBody.substring(start, end) : responseBody.substring(0, Math.min(200, responseBody.length()));
    }
}
